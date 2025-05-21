package com.jcmateus.kalisfit.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.result.launch
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // --- Lógica de Permisos (puede permanecer en la UI o manejarse aquí para iniciar el servicio) ---
    private val _hasLocationPermission = MutableStateFlow(checkInitialPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters: Float = 0f

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(getApplication<Application>().applicationContext)
    }

    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback

    private fun checkInitialPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication<Application>().applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateLocationPermission(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted && _activityState.value == ActivityState.RUNNING) {
            startLocationUpdates()
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
            _currentLocation.value = null // Resetear la ubicación actual
            lastLocation = null
            totalDistanceMeters = 0f
            startTimer()
            startLocationUpdates()
        } else {
            // La UI debería manejar la solicitud de permisos
            // Podrías emitir un evento para que la UI lo haga si la lógica de permisos está aquí
        }
    }

    fun onPauseClicked() {
        _activityState.value = ActivityState.PAUSED
        timerJob?.cancel()
        stopLocationUpdates() // Pausar también las actualizaciones de ubicación
    }

    fun onResumeClicked() {
        _activityState.value = ActivityState.RUNNING
        startTimer()
        if (_hasLocationPermission.value) {
            startLocationUpdates()
        }
    }

    fun onStopClicked() {
        _activityState.value = ActivityState.FINISHED
        timerJob?.cancel()
        stopLocationUpdates()
        calculateFinalMetrics()
        // TODO: Guardar actividad
    }

    fun onSummaryDone() {
        _activityState.value = ActivityState.IDLE
        // Resetear todos los valores para la próxima actividad (opcional, o hacerlo en onStart)
    }

    private fun startTimer() {
        timerJob?.cancel() // Asegurar que no haya múltiples timers
        timerJob = viewModelScope.launch {
            while (_activityState.value == ActivityState.RUNNING) {
                delay(1000)
                _elapsedTimeSeconds.value++
                // Podrías calcular calorías aquí periódicamente si lo deseas
                _caloriesBurned.value = calculateCalories(_elapsedTimeSeconds.value, _distanceKm.value)
            }
        }
    }

    private fun startLocationUpdates() {
        if (!_hasLocationPermission.value) return

        val locationRequest = LocationRequest.Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Actualizaciones cada 5 seg
            .setMinUpdateIntervalMillis(2000L) // Mínimo cada 2 seg
            .build()

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = newLatLng

                    if (_activityState.value == ActivityState.RUNNING) {
                        _routePoints.value = _routePoints.value + newLatLng

                        lastLocation?.let { previousLocation ->
                            totalDistanceMeters += previousLocation.distanceTo(location)
                            _distanceKm.value = (totalDistanceMeters / 1000.0)
                            calculateCurrentPace()
                        }
                        lastLocation = location
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // La UI debería haber manejado el permiso, pero es bueno tener un catch
            _hasLocationPermission.value = false // Marcar que no tenemos permiso si falla
            // Podrías emitir un error a la UI
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) { // Verificar si fue inicializado antes de removerlo
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun calculateCurrentPace() {
        if (_distanceKm.value > 0 && _elapsedTimeSeconds.value > 0) {
            val paceMinPerKm = (_elapsedTimeSeconds.value / 60.0) / _distanceKm.value
            if (paceMinPerKm.isFinite() && paceMinPerKm > 0) {
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
        // El ritmo promedio ya se habrá ido calculando, pero podrías recalcularlo aquí
        // con el total de distancia y tiempo si prefieres.
        // Las calorías también se habrán ido actualizando.
        if (_distanceKm.value > 0 && _elapsedTimeSeconds.value > 0) {
            val avgPaceMinPerKm = (_elapsedTimeSeconds.value / 60.0) / _distanceKm.value
            if (avgPaceMinPerKm.isFinite() && avgPaceMinPerKm > 0) {
                val minutes = avgPaceMinPerKm.toInt()
                val seconds = ((avgPaceMinPerKm - minutes) * 60).toInt()
                // Podrías tener un StateFlow para avgPace si lo quieres mostrar en el resumen específicamente
                // _avgPace.value = String.format("%d:%02d /km", minutes, seconds)
                // Por ahora, el _currentPace se actualiza en tiempo real, así que al final será el promedio.
            }
        }
    }

    private fun calculateCalories(timeSeconds: Long, distanceKm: Double): Int {
        // Fórmula de ejemplo muy simple. Deberías usar una más precisa.
        // MET (Metabolic Equivalent of Task) para correr es aprox. 7-12 dependiendo de la intensidad.
        // Calorías = METs * peso_corporal_kg * tiempo_en_horas
        // Asumamos un MET de 8 y un peso de 70kg para el placeholder
        val mets = 8.0
        val weightKg = 70.0 // Deberías obtener esto del perfil del usuario
        val timeHours = timeSeconds / 3600.0
        return (mets * weightKg * timeHours).toInt()
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates() // Asegurarse de detener las actualizaciones al destruir el ViewModel
        timerJob?.cancel()
    }
}