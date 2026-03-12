package com.jcmateus.kalisfit.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.model.RoutePoint
import com.jcmateus.kalisfit.model.SplitData
import com.jcmateus.kalisfit.model.UserActivity
import com.jcmateus.kalisfit.services.RunningForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estados de la actividad
enum class ActivityState {
    IDLE, RUNNING, PAUSED, FINISHED
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

    private val _avgPace = MutableStateFlow("0:00 /km")
    val avgPace: StateFlow<String> = _avgPace.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(checkInitialPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _splits = MutableStateFlow<List<Split>>(emptyList())
    val splits: StateFlow<List<Split>> = _splits.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

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
    private val MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS = 5f

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dataClient by lazy { Wearable.getDataClient(application) }

    private var userWeightKg: Double = 70.0

    private fun checkInitialPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // --- FUNCIÓN FALTANTE 1 ---
    fun updateLocationPermission(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted) {
            if (_activityState.value == ActivityState.RUNNING) {
                startLocationUpdates()
            }
        } else {
            stopLocationUpdates()
        }
    }

    fun onStartClicked() {
        if (_hasLocationPermission.value) {
            _activityState.value = ActivityState.RUNNING
            resetActivityMetrics()
            startTimer()
            startLocationUpdates()
            startForegroundService()
            Log.d("RunningViewModel", "Actividad iniciada.")
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
            stopLocationUpdates()
            updateForegroundServiceNotification()
            Log.d("RunningViewModel", "Actividad pausada.")
        }
    }

    fun onResumeClicked() {
        if (_activityState.value == ActivityState.PAUSED) {
            _activityState.value = ActivityState.RUNNING
            startTimer()
            if (_hasLocationPermission.value) {
                startLocationUpdates()
            }
            updateForegroundServiceNotification()
            Log.d("RunningViewModel", "Actividad reanudada.")
        }
    }

    fun onStopClicked(saveActivity: Boolean) {
        if (_activityState.value == ActivityState.RUNNING || _activityState.value == ActivityState.PAUSED) {
            timerJob?.cancel()
            stopLocationUpdates()
            stopForegroundService()

            if (saveActivity) {
                calculateFinalMetricsAndSplits()
                if (_elapsedTimeSeconds.value >= 10 || _routePoints.value.size >= 3) {
                    saveActivityToFirestore()
                }
            }
            _activityState.value = ActivityState.FINISHED
        }
    }

    // --- FUNCIÓN FALTANTE 2 ---
    fun onSummaryDone() {
        _activityState.value = ActivityState.IDLE
        resetActivityMetrics()
        Log.d("RunningViewModel", "Resumen finalizado, volviendo a IDLE.")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_activityState.value == ActivityState.RUNNING) {
                delay(1000)
                _elapsedTimeSeconds.value++
                sendDataToWearable(_elapsedTimeSeconds.value, _distanceKm.value)
                _caloriesBurned.value = calculateCalories(_elapsedTimeSeconds.value, _distanceKm.value, userWeightKg)
                updateForegroundServiceNotification()
            }
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(application, RunningForegroundService::class.java).apply {
            action = RunningForegroundService.ACTION_START_OR_RESUME_SERVICE
        }
        ContextCompat.startForegroundService(application, serviceIntent)
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(application, RunningForegroundService::class.java).apply {
            action = RunningForegroundService.ACTION_STOP_SERVICE
        }
        application.stopService(serviceIntent)
    }

    private fun updateForegroundServiceNotification() {
        val serviceIntent = Intent(application, RunningForegroundService::class.java).apply {
            action = RunningForegroundService.ACTION_UPDATE_NOTIFICATION
            putExtra("elapsedTime", formatElapsedTime(_elapsedTimeSeconds.value))
            putExtra("distanceKm", String.format("%.2f km", _distanceKm.value))
        }
        application.startService(serviceIntent)
    }

    private fun formatElapsedTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun sendDataToWearable(time: Long, distance: Double) {
        val putDataMapReq = PutDataMapRequest.create("/running_metrics").apply {
            dataMap.putLong("elapsedTime", time)
            dataMap.putDouble("distance", distance)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        dataClient.putDataItem(putDataMapReq.asPutDataRequest().setUrgent())
    }

    private fun startLocationUpdates() {
        if (!_hasLocationPermission.value) return
        
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

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2500L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            _hasLocationPermission.value = false
        }
    }

    private fun processNewLocation(location: Location, newLatLng: LatLng) {
        var shouldAddPointToRoute = _routePoints.value.isEmpty()
        if (!shouldAddPointToRoute) {
            val lastSaved = Location("").apply {
                latitude = lastSavedRoutePointForFirestore!!.latitude
                longitude = lastSavedRoutePointForFirestore!!.longitude
            }
            if (lastSaved.distanceTo(location) >= MIN_DISTANCE_BETWEEN_ROUTE_POINTS_METERS) {
                shouldAddPointToRoute = true
            }
        }

        if (shouldAddPointToRoute) {
            _routePoints.value = _routePoints.value + newLatLng
            lastSavedRoutePointForFirestore = newLatLng
        }

        lastLocation?.let { previous ->
            val distanceIncrement = previous.distanceTo(location)
            if (distanceIncrement > 0.5f && distanceIncrement < 500f) {
                totalDistanceMeters += distanceIncrement
                _distanceKm.value = totalDistanceMeters / 1000.0
                calculateCurrentPace()
                checkAndRecordSplit(_distanceKm.value, _elapsedTimeSeconds.value)
            }
        }
        lastLocation = location
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun formatPace(timeSeconds: Long, distanceKm: Double): String {
        if (distanceKm > 0.01 && timeSeconds > 0) {
            val paceMinPerKm = (timeSeconds / 60.0) / distanceKm
            if (paceMinPerKm < 60) {
                val minutes = paceMinPerKm.toInt()
                val seconds = ((paceMinPerKm - minutes) * 60).toInt()
                return String.format("%d:%02d /km", minutes, seconds)
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
            val paceForThisKm = formatPace(timeForThisKm, 1.0)
            _splits.value = _splits.value + Split(nextSplitKm, timeForThisKm, paceForThisKm)
            timeAtLastSplitSeconds = currentTimeSeconds
            nextSplitKm++
        }
    }

    private fun calculateFinalMetricsAndSplits() {
        _avgPace.value = formatPace(_elapsedTimeSeconds.value, _distanceKm.value)
        val remainingDistanceKm = _distanceKm.value - (nextSplitKm - 1)
        if (remainingDistanceKm > 0.05) {
            val timeForThisKm = _elapsedTimeSeconds.value - timeAtLastSplitSeconds
            val paceForThisKm = formatPace(timeForThisKm, remainingDistanceKm)
            _splits.value = _splits.value + Split(nextSplitKm, timeForThisKm, paceForThisKm)
        }
    }

    private fun saveActivityToFirestore() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val activity = UserActivity(
                    userId = userId,
                    elapsedTimeSeconds = _elapsedTimeSeconds.value,
                    distanceKm = _distanceKm.value,
                    avgPace = _avgPace.value,
                    caloriesBurned = _caloriesBurned.value,
                    routePoints = _routePoints.value.map { RoutePoint(it.latitude, it.longitude) },
                    splits = _splits.value.map { SplitData(it.km, it.timeSeconds, it.pace) }
                )
                db.collection("users").document(userId).collection("activities").add(activity).await()
            } catch (e: Exception) {
                Log.e("RunningViewModel", "Error saving activity", e)
            }
        }
    }

    private fun calculateCalories(time: Long, distance: Double, weight: Double): Int {
        if (time == 0L || distance == 0.0) return 0
        return (9.8 * weight * (time / 3600.0)).toInt()
    }
}