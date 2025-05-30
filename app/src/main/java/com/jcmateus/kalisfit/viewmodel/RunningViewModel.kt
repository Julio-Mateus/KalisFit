package com.jcmateus.kalisfit.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.activity.result.launch
import androidx.compose.foundation.layout.add
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback // Import correcto
import com.google.android.gms.location.LocationRequest  // Import correcto
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority         // Import correcto
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.model.RoutePoint
import com.jcmateus.kalisfit.model.SplitData
import com.jcmateus.kalisfit.model.UserActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estados de la actividad (puede estar aquí o en un archivo común si se usa en más sitios)
enum class ActivityState {
    IDLE,       // Esperando para iniciar
    RUNNING,    // Actividad en curso
    PAUSED,     // Actividad pausada
    FINISHED    // Actividad finalizada (mostrando resumen)
}

data class Split(val km: Int, val timeSeconds: Long, val pace: String)

class RunningViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _activityState = MutableStateFlow(ActivityState.IDLE)
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _currentPace = MutableStateFlow("0:00 /km")
    val currentPace: StateFlow<String> = _currentPace.asStateFlow()

    private val _avgPace = MutableStateFlow("0:00 /km") // Para el resumen
    val avgPace: StateFlow<String> = _avgPace.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(checkInitialPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // --- NUEVAS MÉTRICAS Y DATOS ---
    private val _splits = MutableStateFlow<List<Split>>(emptyList())
    val splits: StateFlow<List<Split>> = _splits.asStateFlow()

    // --- Placeholder para Frecuencia Cardíaca ---
    // private val _heartRate = MutableStateFlow<Int?>(null)
    // val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()
    // TODO: Integrar con Health Connect o APIs de wearables para obtener datos de FC.
    // Necesitarás permisos adicionales (BODY_SENSORS) y lógica para conectar/leer del sensor.
    // Ejemplo de cómo podrías actualizarlo:
    // fun updateHeartRate(newRate: Int) { _heartRate.value = newRate }

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters: Float = 0f
    private var nextSplitKm = 1
    private var timeAtLastSplitSeconds = 0L


    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(application.applicationContext)
    }

    private lateinit var locationCallback: LocationCallback
    private var lastSavedRoutePointForFirestore: LatLng? = null
    private val MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS = 5f // Guardar punto de ruta cada 5 metros (ajustar)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // TODO: Obtener el peso real del perfil del usuario desde Firestore u otra fuente
    private var userWeightKg: Double = 70.0 // Default, actualizar desde perfil

    init {
        // fetchUserWeight() // TODO: Cargar el peso del usuario al iniciar el ViewModel
        // Ejemplo:
        // viewModelScope.launch {
        //     val userId = auth.currentUser?.uid
        //     if (userId != null) {
        //         try {
        //             val userDoc = db.collection("users").document(userId).get().await()
        //             userWeightKg = userDoc.getDouble("weightKg") ?: 70.0
        //         } catch (e: Exception) {
        //             Log.e("RunningViewModel", "Error fetching user weight", e)
        //         }
        //     }
        // }
    }

    private fun checkInitialPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateLocationPermission(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted) {
            if (_activityState.value == ActivityState.RUNNING) {
                startLocationUpdates()
            }
        } else {
            stopLocationUpdates()
            if (_activityState.value == ActivityState.RUNNING || _activityState.value == ActivityState.PAUSED) {
                // Si se revoca el permiso durante una actividad, es buena idea pausarla o detenerla.
                // Aquí podrías forzar una pausa.
                Log.w("RunningViewModel", "Permiso de ubicación revocado durante actividad.")
                // onPauseClicked() // O manejar de otra forma
            }
        }
    }

    fun onStartClicked() {
        if (_hasLocationPermission.value) {
            // TODO: Comprobar permiso de notificación para Foreground Service en Android 13+
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //     if (ContextCompat.checkSelfPermission(application, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            //         // La UI debería solicitar este permiso ANTES de llamar a onStartClicked o aquí emitir un evento para solicitarlo
            //         Log.w("RunningViewModel", "Permiso de notificación necesario para el servicio en primer plano.")
            //         // _requestNotificationPermissionEvent.value = true // Ejemplo de evento
            //         return
            //     }
            // }

            _activityState.value = ActivityState.RUNNING
            resetActivityMetrics()
            startTimer()
            startLocationUpdates()
            // TODO: Iniciar Foreground Service
            // startForegroundService()
            Log.d("RunningViewModel", "Actividad iniciada.")
        } else {
            Log.w("RunningViewModel", "Permiso de ubicación no concedido al intentar iniciar.")
            // La UI debe solicitar permisos (ya lo hace)
        }
    }

    private fun resetActivityMetrics() {
        _elapsedTimeSeconds.value = 0L
        _distanceKm.value = 0.0
        _currentPace.value = "0:00 /km"
        _avgPace.value = "0:00 /km"
        _caloriesBurned.value = 0
        _routePoints.value = emptyList()
        _currentLocation.value = null
        // _heartRate.value = null // Resetear FC
        _splits.value = emptyList()
        lastLocation = null
        totalDistanceMeters = 0f
        nextSplitKm = 1
        timeAtLastSplitSeconds = 0L
        lastSavedRoutePointForFirestore = null
    }

    fun onPauseClicked() {
        if (_activityState.value == ActivityState.RUNNING) {
            _activityState.value = ActivityState.PAUSED
            timerJob?.cancel()
            // Las actualizaciones de ubicación pueden o no detenerse en pausa, depende de si quieres
            // seguir mostrando la ubicación actual en el mapa. Si el ForegroundService las maneja,
            // él podría seguir activo. Por ahora, las detenemos para simplificar.
            stopLocationUpdates() // Considerar si esto es deseado o si el ForegroundService sigue activo.
            // TODO: Actualizar estado del Foreground Service si es necesario (ej. cambiar notificación)
            Log.d("RunningViewModel", "Actividad pausada.")
        }
    }

    fun onResumeClicked() {
        if (_activityState.value == ActivityState.PAUSED) {
            _activityState.value = ActivityState.RUNNING
            startTimer() // Reanuda el contador desde donde se quedó
            if (_hasLocationPermission.value) {
                startLocationUpdates() // Reanuda la obtención de puntos de ruta y cálculos
            }
            // TODO: Actualizar estado del Foreground Service si es necesario
            Log.d("RunningViewModel", "Actividad reanudada.")
        }
    }

    fun onStopClicked(saveActivity: Boolean) { // Añadido parámetro para guardar/descartar
        if (_activityState.value == ActivityState.RUNNING || _activityState.value == ActivityState.PAUSED) {
            timerJob?.cancel()
            stopLocationUpdates()
            // TODO: Detener Foreground Service
            // stopForegroundService()

            if (saveActivity) {
                // Solo calcular métricas finales y guardar si el usuario elige guardar.
                calculateFinalMetricsAndSplits() // Asegura que _avgPace y _splits sean los finales
                // Considerar si guardar una actividad muy corta
                if (_elapsedTimeSeconds.value < 10 && _routePoints.value.size < 3) { // Ejemplo: mínimo 10s y 3 puntos
                    Log.i("RunningViewModel", "Actividad demasiado corta, no se guardará aunque el usuario quiso.")
                    // Se podría mostrar un mensaje al usuario.
                    // Forzar el estado a FINISHED para el resumen, pero no se guardará.
                } else {
                    saveActivityToFirestore()
                }
            } else {
                // Si se descarta, igual mostrar el resumen con los datos actuales, pero no guardar.
                // Las métricas como avgPace podrían no ser las "finales calculadas" si no se llama a calculateFinalMetricsAndSplits()
                // pero currentPace y otras métricas en vivo estarán disponibles.
                // Opcionalmente, podrías calcularlas igualmente para el resumen pero sin guardarlas.
                // Por simplicidad, aquí solo mostramos lo que hay.
                Log.i("RunningViewModel", "Actividad detenida y descartada por el usuario.")
            }
            _activityState.value = ActivityState.FINISHED // Siempre ir al resumen
        }
    }

    fun onSummaryDone() {
        _activityState.value = ActivityState.IDLE
        resetActivityMetrics() // Opcional: Resetear valores aquí si no se hace en onStartClicked completamente
        Log.d("RunningViewModel", "Resumen finalizado, volviendo a IDLE.")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_activityState.value == ActivityState.RUNNING) {
                delay(1000)
                _elapsedTimeSeconds.value++
                _caloriesBurned.value = calculateCalories(_elapsedTimeSeconds.value, _distanceKm.value, userWeightKg)
                // TODO: Actualizar notificación del Foreground Service con elapsedTime
                // updateForegroundServiceNotification()
            }
        }
    }

    private fun startLocationUpdates() {
        if (!_hasLocationPermission.value) {
            Log.d("RunningViewModel", "Intento de iniciar actualizaciones de ubicación sin permiso.")
            return
        }
        if (!::locationCallback.isInitialized) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val newLatLng = LatLng(location.latitude, location.longitude)
                        _currentLocation.value = newLatLng

                        if (_activityState.value == ActivityState.RUNNING) {
                            processNewLocation(location, newLatLng)
                        }
                    }
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Actualizaciones cada 5s
            .setMinUpdateIntervalMillis(2500L) // Mínimo cada 2.5 seg
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d("RunningViewModel", "Actualizaciones de ubicación iniciadas.")
        } catch (e: SecurityException) {
            Log.e("RunningViewModel", "Error de seguridad al solicitar actualizaciones de ubicación.", e)
            _hasLocationPermission.value = false
            // Podrías emitir un evento a la UI para manejar este error.
        }
    }

    private fun processNewLocation(location: Location, newLatLng: LatLng) {
        // ---- Lógica de recolección de puntos para la RUTA de Firestore ----
        var shouldAddPointToRoute = _routePoints.value.isEmpty() // Siempre añadir el primer punto
        if (!shouldAddPointToRoute) {
            val lastSavedLocationForCalc = Location("").apply {
                latitude = lastSavedRoutePointForFirestore!!.latitude // !! es seguro si _routePoints no está vacío
                longitude = lastSavedRoutePointForFirestore!!.longitude
            }
            if (lastSavedLocationForCalc.distanceTo(location) >= MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS) {
                shouldAddPointToRoute = true
            }
        }

        if (shouldAddPointToRoute) {
            _routePoints.value = _routePoints.value + newLatLng
            lastSavedRoutePointForFirestore = newLatLng
        }

        // Lógica de cálculo de distancia
        lastLocation?.let { previousLocation ->
            // Usar el objeto Location directamente para el cálculo de distancia,
            // ya que el newLatLng podría ser un punto filtrado para la ruta guardada,
            // pero la distancia debe ser con la ubicación real.
            val distanceIncrement = previousLocation.distanceTo(location)

            // Filtrar pequeños movimientos o ruido GPS, y movimientos hacia atrás (poco probable pero posible)
            if (distanceIncrement > 0.5f && distanceIncrement < 500f) { // Umbral mínimo y máximo razonable por actualización
                totalDistanceMeters += distanceIncrement
                val newDistanceKm = totalDistanceMeters / 1000.0
                _distanceKm.value = newDistanceKm
                calculateCurrentPace()
                checkAndRecordSplit(newDistanceKm, _elapsedTimeSeconds.value)
            }
        }
        lastLocation = location // Actualizar siempre lastLocation con la ubicación real
    }


    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.d("RunningViewModel", "Actualizaciones de ubicación detenidas.")
            } catch (e: Exception) {
                Log.e("RunningViewModel", "Error al detener actualizaciones de ubicación.", e)
            }
        }
    }

    private fun formatPace(timeSeconds: Long, distanceKm: Double): String {
        if (distanceKm > 0.01 && timeSeconds > 0) {
            val paceMinPerKm = (timeSeconds / 60.0) / distanceKm
            if (paceMinPerKm.isFinite() && paceMinPerKm > 0 && paceMinPerKm < 60) { // Ritmo razonable
                val minutes = paceMinPerKm.toInt()
                val secondsPart = ((paceMinPerKm - minutes) * 60).toInt()
                return String.format("%d:%02d /km", minutes, secondsPart)
            } else {
                return "--:-- /km"
            }
        }
        return "0:00 /km"
    }

    private fun calculateCurrentPace() {
        _currentPace.value = formatPace(_elapsedTimeSeconds.value, _distanceKm.value)
    }

    private fun checkAndRecordSplit(currentDistanceKm: Double, currentTimeSeconds: Long) {
        if (currentDistanceKm >= nextSplitKm) {
            val timeForThisKm = currentTimeSeconds - timeAtLastSplitSeconds
            val paceForThisKm = formatPace(timeForThisKm, 1.0) // 1.0 porque es para 1 km
            _splits.value = _splits.value + Split(km = nextSplitKm, timeSeconds = timeForThisKm, pace = paceForThisKm)

            // TODO: Considerar Audio Cue para el split
            // triggerAudioCueForSplit(nextSplitKm, timeForThisKm, paceForThisKm)

            timeAtLastSplitSeconds = currentTimeSeconds
            nextSplitKm++
        }
    }

    private fun calculateFinalMetricsAndSplits() {
        // Calcular ritmo promedio final
        _avgPace.value = formatPace(_elapsedTimeSeconds.value, _distanceKm.value)

        // Asegurar que el último split parcial se registre si la actividad no terminó exactamente en un km
        // Esto es opcional, algunas apps solo muestran splits completos.
        // Si quieres el split parcial:
        val remainingDistanceKm = _distanceKm.value - (nextSplitKm - 1)
        if (remainingDistanceKm > 0.01 && _activityState.value != ActivityState.IDLE) { // Evitar si ya se reseteó
            val timeForPartialKm = _elapsedTimeSeconds.value - timeAtLastSplitSeconds
            if (timeForPartialKm > 0) {
                // El "pace" de un split parcial puede ser engañoso si se muestra como "/km"
                // Podrías mostrar solo el tiempo para esa fracción o calcular el pace normalizado.
                // Aquí lo calculamos como si fuera un km completo, lo que puede ser confuso.
                // Una mejor aproximación sería el pace actual al momento de parar.
                // O simplemente no añadir este split parcial.
                // val paceForPartialKm = formatPace(timeForPartialKm, remainingDistanceKm)
                // Log.d("RunningViewModel", "Último split parcial (dist: $remainingDistanceKm km, tiempo: $timeForPartialKm s)")
                // Por ahora, no añadiremos splits parciales para evitar confusión en la UI.
            }
        }
        // Las calorías ya se actualizan en el timer, así que _caloriesBurned.value es el total.
    }

    private fun saveActivityToFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("RunningViewModel", "Usuario no autenticado. No se puede guardar la actividad.")
            return
        }

        val activityRoutePoints = _routePoints.value.map { latLng ->
            RoutePoint(latitude = latLng.latitude, longitude = latLng.longitude)
        }

        // Usar el valor de _avgPace que fue actualizado por calculateFinalMetricsAndSplits()
        val finalAvgPaceToSave = _avgPace.value

        val activityToSave = UserActivity(
            userId = currentUser.uid,
            // 'timestamp' será establecido por @ServerTimestamp en el modelo UserActivity
            elapsedTimeSeconds = _elapsedTimeSeconds.value,
            distanceKm = _distanceKm.value,
            avgPace = finalAvgPaceToSave,
            caloriesBurned = _caloriesBurned.value,
            routePoints = activityRoutePoints,
            splits = _splits.value.map { viewModelSplit -> // <--- Mapeo de ViewModel.Split a UserActivity.SplitData
                SplitData(km = viewModelSplit.km, timeSeconds = viewModelSplit.timeSeconds, pace = viewModelSplit.pace)
            } // Mapear a un sub-modelo si es necesario
            // mapImageUrl puedes generarlo y asignarlo aquí si tienes esa funcionalidad
        )

        viewModelScope.launch {
            try {
                db.collection("users").document(currentUser.uid)
                    .collection("activities")
                    .add(activityToSave)
                    .await()
                Log.d("RunningViewModel", "Actividad guardada exitosamente en Firestore.")
            } catch (e: Exception) {
                Log.e("RunningViewModel", "Error al guardar actividad en Firestore", e)
                // Emitir un evento/estado a la UI para informar del error
                // _saveErrorEvent.value = "Error al guardar: ${e.message}"
            }
        }
    }

    private fun calculateCalories(timeSeconds: Long, distanceKm: Double, weightKg: Double): Int {
        // Fórmula METs simplificada para correr: METs varían con la velocidad.
        // Una aproximación es que correr a ~10 min/milla (6 mph o ~9.65 km/h) son ~10 METs.
        // A ~12 min/milla (5 mph o ~8 km/h) son ~8.5 METs.
        // Si no tenemos velocidad/pace fácil aquí, usamos un promedio.
        // Otra fórmula: Calorías ≈ distancia (km) × peso (kg) × 1.036 (factor para correr)
        // Esta es más simple si tenemos distancia.

        if (distanceKm > 0) {
            return (distanceKm * weightKg * 1.036).toInt()
        }
        // Si no hay distancia, podemos usar una basada en tiempo y METs promedio si es necesario.
        val metsApproximation = 8.0 // METs para correr moderado, ajustar según sea necesario
        val timeHours = timeSeconds / 3600.0
        return (metsApproximation * weightKg * timeHours).toInt()
    }

    // --- Foreground Service Management (Placeholders) ---
    private fun startForegroundService() {
        // TODO:
        // val serviceIntent = Intent(application, RunningForegroundService::class.java)
        // serviceIntent.action = RunningForegroundService.ACTION_START_OR_RESUME_SERVICE
        // ContextCompat.startForegroundService(application, serviceIntent)
        Log.d("RunningViewModel", "Solicitando inicio de Foreground Service...")
    }

    private fun stopForegroundService() {
        // TODO:
        // val serviceIntent = Intent(application, RunningForegroundService::class.java)
        // serviceIntent.action = RunningForegroundService.ACTION_STOP_SERVICE
        // ContextCompat.startForegroundService(application, serviceIntent)
        Log.d("RunningViewModel", "Solicitando detención de Foreground Service...")
    }

    // private fun updateForegroundServiceNotification() {
    // TODO:
    //     if (_activityState.value == ActivityState.RUNNING) {
    //         val serviceIntent = Intent(application, RunningForegroundService::class.java)
    //         serviceIntent.action = RunningForegroundService.ACTION_UPDATE_NOTIFICATION
    //         serviceIntent.putExtra("elapsedTime", _elapsedTimeSeconds.value)
    //         serviceIntent.putExtra("distanceKm", _distanceKm.value)
    //         application.startService(serviceIntent) // No necesita ser startForegroundService para solo actualizar
    //     }
    // }

    // --- Audio Cues (Placeholder) ---
    // private fun triggerAudioCueForSplit(km: Int, timeSeconds: Long, pace: String) {
    // TODO: Implementar TextToSpeech
    //     val message = "Kilómetro $km completado en ${formatElapsedTime(timeSeconds)}, ritmo $pace."
    //     Log.d("RunningViewModel", "Audio Cue: $message")
    //     // tts.speak(message, TextToSpeech.QUEUE_ADD, null, null)
    // }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates() // Asegurarse de limpiar
        timerJob?.cancel()
        // TODO: Si el ForegroundService no se detiene automáticamente al detener la actividad,
        // considerar si debe detenerse aquí o si el servicio gestiona su propio ciclo de vida.
        // stopForegroundService() // Podría ser necesario si el servicio sigue corriendo.
        Log.d("RunningViewModel", "ViewModel cleared.")
    }
}