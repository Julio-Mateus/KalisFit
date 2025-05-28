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

class RunningViewModel(application: Application) : AndroidViewModel(application) {

    private val _activityState = MutableStateFlow(ActivityState.IDLE)
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _currentPace = MutableStateFlow("0:00 /km") // Ritmo por km
    val currentPace: StateFlow<String> = _currentPace.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(checkInitialPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters: Float = 0f

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(getApplication<Application>().applicationContext)
    }

    private lateinit var locationCallback: LocationCallback
    private var lastSavedRoutePointForFirestore: LatLng? = null // Para filtrar puntos para Firestore
    private val MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS = 5f // Ejemplo: guardar un punto c

    // Instancias de Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }




    private fun checkInitialPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication<Application>().applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateLocationPermission(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted && (_activityState.value == ActivityState.RUNNING || _activityState.value == ActivityState.PAUSED)) { // Corregido para iniciar también si estaba pausado y se otorga permiso
            if (_activityState.value == ActivityState.RUNNING) { // Solo si estaba corriendo activamente
                startLocationUpdates()
            }
        } else if (!isGranted) {
            stopLocationUpdates()
        }
    }

    fun onStartClicked() {
        if (_hasLocationPermission.value) {
            _activityState.value = ActivityState.RUNNING
            _elapsedTimeSeconds.value = 0L
            _distanceKm.value = 0.0
            _currentPace.value = "0:00 /km"
            _caloriesBurned.value = 0
            _routePoints.value = emptyList()
            _currentLocation.value = null
            lastLocation = null
            totalDistanceMeters = 0f
            startTimer()
            startLocationUpdates()
        } else {
            // La UI debería solicitar permisos.
            // Puedes emitir un evento/estado para que la UI lo haga.
            Log.w("RunningViewModel", "Permiso de ubicación no concedido al intentar iniciar.")
        }
    }

    fun onPauseClicked() {
        if (_activityState.value == ActivityState.RUNNING) { // Solo pausar si está corriendo
            _activityState.value = ActivityState.PAUSED
            timerJob?.cancel()
            stopLocationUpdates()
        }
    }

    fun onResumeClicked() {
        if (_activityState.value == ActivityState.PAUSED) { // Solo reanudar si está pausado
            _activityState.value = ActivityState.RUNNING
            startTimer()
            if (_hasLocationPermission.value) {
                startLocationUpdates()
            }
        }
    }

    fun onStopClicked() {
        if (_activityState.value == ActivityState.RUNNING || _activityState.value == ActivityState.PAUSED) {
            _activityState.value = ActivityState.FINISHED
            timerJob?.cancel()
            stopLocationUpdates()
            calculateFinalMetrics() // Asegura que _currentPace sea el promedio final
            saveActivityToFirestore() // Guardar la actividad
        }
    }

    fun onSummaryDone() {
        _activityState.value = ActivityState.IDLE
        // Opcional: Resetear valores aquí si no se hace en onStartClicked
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_activityState.value == ActivityState.RUNNING) {
                delay(1000)
                _elapsedTimeSeconds.value++
                _caloriesBurned.value = calculateCalories(_elapsedTimeSeconds.value, _distanceKm.value)
            }
        }
    }

    private fun startLocationUpdates() {
        if (!_hasLocationPermission.value) {
            Log.d("RunningViewModel", "Intento de iniciar actualizaciones de ubicación sin permiso.")
            return
        }
        if (!::locationCallback.isInitialized) { // Crear callback solo si no existe
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val newLatLng = LatLng(location.latitude, location.longitude)
                        _currentLocation.value = newLatLng

                        if (_activityState.value == ActivityState.RUNNING) {
                            // ---- INICIO: Lógica de recolección de puntos para la RUTA de Firestore ----
                            var shouldAddPointToRoute = false
                            if (lastSavedRoutePointForFirestore == null) {
                                shouldAddPointToRoute = true // Siempre añadir el primer punto
                            } else {
                                val tempLocationForCalc = Location("").apply {
                                    latitude = newLatLng.latitude
                                    longitude = newLatLng.longitude
                                }
                                val lastSavedLocationForCalc = Location("").apply {
                                    latitude = lastSavedRoutePointForFirestore!!.latitude
                                    longitude = lastSavedRoutePointForFirestore!!.longitude
                                }
                                if (lastSavedLocationForCalc.distanceTo(tempLocationForCalc) >= MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS) {
                                    shouldAddPointToRoute = true
                                }
                            }

                            if (shouldAddPointToRoute) {
                                _routePoints.value = _routePoints.value + newLatLng // Añade a la lista que se guardará
                                lastSavedRoutePointForFirestore = newLatLng
                            }
                            // ---- FIN: Lógica de recolección de puntos para la RUTA de Firestore ----


                            // Lógica de cálculo de distancia (puede usar todos los puntos o los filtrados)
                            lastLocation?.let { previousLocation ->
                                val distanceIncrement = previousLocation.distanceTo(location)
                                if (distanceIncrement > 0.5f) { // Evitar pequeños saltos o ruido
                                    totalDistanceMeters += distanceIncrement
                                    _distanceKm.value = (totalDistanceMeters / 1000.0)
                                    calculateCurrentPace()
                                }
                            }
                            lastLocation = location
                        }
                    }
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
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

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("RunningViewModel", "Actualizaciones de ubicación detenidas.")
        }
    }

    private fun calculateCurrentPace() {
        if (_distanceKm.value > 0.01 && _elapsedTimeSeconds.value > 0) { // Un pequeño umbral para distancia
            val paceMinPerKm = (_elapsedTimeSeconds.value / 60.0) / _distanceKm.value
            if (paceMinPerKm.isFinite() && paceMinPerKm > 0 && paceMinPerKm < 60) { // Ritmo razonable (menos de 1h/km)
                val minutes = paceMinPerKm.toInt()
                val seconds = ((paceMinPerKm - minutes) * 60).toInt()
                _currentPace.value = String.format("%d:%02d /km", minutes, seconds)
            } else {
                _currentPace.value = "--:-- /km"
            }
        } else {
            _currentPace.value = "0:00 /km"
        }
    }

    private fun calculateFinalMetrics() {
        // Esta función recalcula el ritmo basado en el total, asegurando que _currentPace sea el promedio final
        if (_distanceKm.value > 0.01 && _elapsedTimeSeconds.value > 0) {
            val avgPaceMinPerKm = (_elapsedTimeSeconds.value / 60.0) / _distanceKm.value
            if (avgPaceMinPerKm.isFinite() && avgPaceMinPerKm > 0 && avgPaceMinPerKm < 60) {
                val minutes = avgPaceMinPerKm.toInt()
                val seconds = ((avgPaceMinPerKm - minutes) * 60).toInt()
                _currentPace.value = String.format("%d:%02d /km", minutes, seconds)
            } else {
                // Si el cálculo final no es válido, mantener el último valor calculado o un placeholder
                if (_currentPace.value == "0:00 /km" || _currentPace.value == "--:-- /km") {
                    _currentPace.value = "--:-- /km" // O un valor por defecto si no hay ritmo previo
                }
            }
        } else {
            _currentPace.value = "0:00 /km" // O "--:-- /km" si no hubo actividad significativa
        }
        // Las calorías ya se actualizan en el timer, así que _caloriesBurned.value es el total.
    }

    private fun saveActivityToFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("RunningViewModel", "Usuario no autenticado. No se puede guardar la actividad.")
            // Considera emitir un evento/estado a la UI para informar al usuario
            return
        }

        // Convertir List<com.google.android.gms.maps.model.LatLng> a List<com.jcmateus.kalisfit.model.RoutePoint>
        val activityRoutePoints = _routePoints.value.map { latLng ->
            RoutePoint(latitude = latLng.latitude, longitude = latLng.longitude)
        }

        // Usar el valor de _currentPace que fue actualizado por calculateFinalMetrics()
        val finalAvgPace = _currentPace.value

        // Solo guardar si hay alguna distancia o tiempo significativo (opcional, pero recomendado)
        if (_elapsedTimeSeconds.value < 5 && activityRoutePoints.size < 2) {
            Log.i("RunningViewModel", "Actividad demasiado corta, no se guardará.")
            // Puedes cambiar _activityState a IDLE directamente si no hay resumen para actividades no guardadas
            // _activityState.value = ActivityState.IDLE
            return
        }


        val activityToSave = UserActivity(
            userId = currentUser.uid,
            // 'timestamp' será establecido por @ServerTimestamp en el modelo UserActivity
            elapsedTimeSeconds = _elapsedTimeSeconds.value,
            distanceKm = _distanceKm.value,
            avgPace = finalAvgPace,
            caloriesBurned = _caloriesBurned.value,
            routePoints = activityRoutePoints
            // mapImageUrl puedes generarlo y asignarlo aquí si tienes esa funcionalidad
        )

        viewModelScope.launch {
            try {
                db.collection("users").document(currentUser.uid)
                    .collection("activities")
                    .add(activityToSave)
                    .await()
                Log.d("RunningViewModel", "Actividad guardada exitosamente en Firestore.")
                // Aquí el estado ya es FINISHED, la UI debería mostrar el resumen.
                // Si quieres un evento específico de éxito, puedes emitirlo.
            } catch (e: Exception) {
                Log.e("RunningViewModel", "Error al guardar actividad en Firestore", e)
                // Emitir un evento/estado a la UI para informar del error
                // _saveErrorEvent.value = "Error al guardar: ${e.message}"
            }
        }
    }


    private fun calculateCalories(timeSeconds: Long, distanceKm: Double): Int {
        val mets = 8.0 // METs para correr moderado, ajustar según sea necesario
        val weightKg = 70.0 // TODO: Obtener esto del perfil del usuario
        val timeHours = timeSeconds / 3600.0
        return (mets * weightKg * timeHours).toInt()
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        timerJob?.cancel()
        Log.d("RunningViewModel", "ViewModel cleared.")
    }
}