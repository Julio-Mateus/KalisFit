package com.jcmateus.kalisfit.viewmodel

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
//import androidx.compose.animation.core.copy
//import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp // <--- AÑADE ESTA LÍNEA
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.DiaDeEntrenamientoPlanificado
import com.jcmateus.kalisfit.model.PlanSemanalUsuario
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.TipoDiaEntrenamiento
import com.jcmateus.kalisfit.model.UserActivity
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Tu data class UserProfile se mantiene igual
data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fechaRegistro: Timestamp? = null,
    val nivel: String = "",
    val objetivos: List<String> = emptyList(),
    val peso: Float = 0f,
    val altura: Float = 0f,
    val edad: Int = 0,
    val sexo: String = "",
    val frecuenciaSemanal: Int = 3,
    val lugarEntrenamiento: List<String> = emptyList(),
    val insignias: List<String> = emptyList(),
    val rutinasCompletadas: Int = 0,
    val progresoActual: String = "",
    val fotoUrl: String = ""
)
// --- Data Class para el Resumen Semanal del HomeScreen ---
data class ResumenSemanal(
    // Datos anteriores renombrados para claridad
    val rutinasCompletadasEstaSemana: Int = 0, // Anteriormente 'rutinasCompletadas'
    val tiempoTotalEntrenadoSegundosEstaSemana: Int = 0, // Anteriormente 'tiempoTotalEntrenadoSegundos'

    // Nuevos campos basados en la discusión
    val frecuenciaSemanalObjetivo: Int = 0, // Tomado de UserProfile.frecuenciaSemanal
    val diasActivosEstaSemana: Int = 0, // Cuántos días distintos tuvo actividad

    // Campos que ya tenías, mantenidos por si los usas o planeas usar
    val objetivosCompletados: List<String> = emptyList(), // Considera renombrar si refleja otra cosa
    val insigniasObtenidas: List<String> = emptyList(),
    val progresoActual: String = "" // Podría tomarse de UserProfile.progresoActual
)
// --- Sealed class para la Última Actividad del HomeScreen ---
sealed class LastActivityItem {
    data class Routine(val progreso: ProgresoRutina) : LastActivityItem()
    data class FreeActivity(val activity: UserActivity) : LastActivityItem()
    object None : LastActivityItem()
    object Loading : LastActivityItem()
}
class UserProfileViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {
    // --- Racha puntos ---
    private val _rachaPuntos = MutableStateFlow(0)
    val rachaPuntos: StateFlow<Int> = _rachaPuntos.asStateFlow()
    private val _rachaActual = MutableStateFlow(0)
    val rachaActual: StateFlow<Int> = _rachaActual.asStateFlow()
    // --- Estados del Perfil del Usuario ---
    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()
    private val _isLoadingUser = MutableStateFlow(false)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()
    private val _userErrorMessage = MutableStateFlow<String?>(null)
    val userErrorMessage: StateFlow<String?> = _userErrorMessage.asStateFlow()
    // --- Estados para Rutinas Recomendadas ---
    private val _recommendedRoutines = MutableStateFlow<List<Rutina>>(emptyList())
    val recommendedRoutines: StateFlow<List<Rutina>> = _recommendedRoutines.asStateFlow()
    private val _routinesErrorMessage = MutableStateFlow<String?>(null)
    val routinesErrorMessage: StateFlow<String?> = _routinesErrorMessage.asStateFlow()
    private val _isLoadingRoutines = MutableStateFlow(false)
    val isLoadingRoutines: StateFlow<Boolean> = _isLoadingRoutines.asStateFlow()
    private val _userCustomRoutines = MutableStateFlow<List<UserCustomRoutine>>(emptyList())
    val userCustomRoutines: StateFlow<List<UserCustomRoutine>> = _userCustomRoutines.asStateFlow()
    private val _isLoadingUserCustomRoutines = MutableStateFlow(false)
    val isLoadingUserCustomRoutines: StateFlow<Boolean> = _isLoadingUserCustomRoutines.asStateFlow()
    private val _userCustomRoutinesError = MutableStateFlow<String?>(null)
    val userCustomRoutinesError: StateFlow<String?> = _userCustomRoutinesError.asStateFlow()
    // --- Estados para los Campos Editables del Perfil ---
    private val _editableNombre = MutableStateFlow("")
    val editableNombre: StateFlow<String> = _editableNombre.asStateFlow()
    private val _editableNivel = MutableStateFlow("")
    val editableNivel: StateFlow<String> = _editableNivel.asStateFlow()
    private val _editableObjetivos = MutableStateFlow<List<String>>(emptyList())
    val editableObjetivos: StateFlow<List<String>> = _editableObjetivos.asStateFlow()
    private val _editablePeso = MutableStateFlow("")
    val editablePeso: StateFlow<String> = _editablePeso.asStateFlow()
    private val _editableAltura = MutableStateFlow("")
    val editableAltura: StateFlow<String> = _editableAltura.asStateFlow()
    private val _editableEdad = MutableStateFlow("")
    val editableEdad: StateFlow<String> = _editableEdad.asStateFlow()
    private val _editableSexo = MutableStateFlow("")
    val editableSexo: StateFlow<String> = _editableSexo.asStateFlow()
    private val _editableFrecuenciaSemanal = MutableStateFlow("")
    val editableFrecuenciaSemanal: StateFlow<String> = _editableFrecuenciaSemanal.asStateFlow()
    private val _editableLugarEntrenamiento = MutableStateFlow<List<String>>(emptyList())
    val editableLugarEntrenamiento: StateFlow<List<String>> = _editableLugarEntrenamiento.asStateFlow()
    // --- Estado para la Operación de Actualización/Guardado del Perfil ---
    sealed class UpdateProfileState {
        object Idle : UpdateProfileState()
        object Loading : UpdateProfileState()
        object Success : UpdateProfileState()
        data class Error(val message: String) : UpdateProfileState()
    }
    private val _updateState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateState: StateFlow<UpdateProfileState> = _updateState.asStateFlow()
    // --- Estados para los Datos del HomeScreen ---
    private val _homeScreenSummary = MutableStateFlow<ResumenSemanal?>(null)
    val homeScreenSummary: StateFlow<ResumenSemanal?> = _homeScreenSummary.asStateFlow()
    private val _lastActivity = MutableStateFlow<LastActivityItem>(LastActivityItem.Loading)
    val lastActivity: StateFlow<LastActivityItem> = _lastActivity.asStateFlow()
    private val _isLoadingHomeScreenData = MutableStateFlow(false)
    val isLoadingHomeScreenData: StateFlow<Boolean> = _isLoadingHomeScreenData.asStateFlow()
    private val _homeScreenErrorMessage = MutableStateFlow<String?>(null)
    val homeScreenErrorMessage: StateFlow<String?> = _homeScreenErrorMessage.asStateFlow()
    private val _planSemanal = MutableStateFlow<PlanSemanalUsuario?>(null)
    val planSemanal: StateFlow<PlanSemanalUsuario?> = _planSemanal.asStateFlow()
    private val _rutinaDeHoy = MutableStateFlow<DiaDeEntrenamientoPlanificado?>(null)
    val rutinaDeHoy: StateFlow<DiaDeEntrenamientoPlanificado?> = _rutinaDeHoy.asStateFlow()
    private val _isLoadingPlanSemanal = MutableStateFlow(false)
    val isLoadingPlanSemanal: StateFlow<Boolean> = _isLoadingPlanSemanal.asStateFlow()
    private val _planSemanalErrorMessage = MutableStateFlow<String?>(null)
    val planSemanalErrorMessage: StateFlow<String?> = _planSemanalErrorMessage.asStateFlow()
    companion object {
        private const val TAG = "UserProfileViewModel"
        // Formateador de fecha para mostrar en la UI si es necesario
        // Asegúrate que tus modelos (ProgresoRutina.fecha, UserActivity.timestamp) se manejen adecuadamente
        // para la visualización. ProgresoRutina.fecha es Timestamp, UserActivity.timestamp es Date?
        @SuppressLint("SimpleDateFormat")
        val displayDateFormatter = SimpleDateFormat("EEE, d MMM yyyy HH:mm",
            Locale.getDefault())

        fun formatFirebaseTimestampForDisplay(timestamp: com.google.firebase.Timestamp?): String {
            return timestamp?.toDate()?.let { displayDateFormatter.format(it) } ?: "N/A"
        }
        fun formatDateForDisplay(date: Date?): String {
            return date?.let { displayDateFormatter.format(it) } ?: "N/A"
        }
    }
    init {
        if (firebaseAuth.currentUser != null) {
            loadUserProfile()
        } else {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false
            _homeScreenErrorMessage.value = "Usuario no autenticado."
            _lastActivity.value = LastActivityItem.None
            _planSemanal.value = null // Limpiar plan si no hay perfil
            _rutinaDeHoy.value = null
        }
    }
    private fun calcularRachaActual(uid: String) {
        viewModelScope.launch {
            try {
                // Obtenemos los últimos entrenamientos de Firestore
                val snapshot = firestore.collection("users").document(uid)
                    .collection("progresoRutinas")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .limit(30)
                    .get()
                    .await()

                val registros = snapshot.toObjects(ProgresoRutina::class.java)

                if (registros.isEmpty()) {
                    _rachaActual.value = 0
                    return@launch
                }

                // Normalizamos las fechas a "solo día" (sin horas) para comparar
                val fechas = registros.map { it.fecha.toDate() }.map { date ->
                    Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                }.distinct()

                // Definimos Hoy y Ayer
                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0);
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time

                val ayer = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0);
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time

                // Si no entrenó ni hoy ni ayer, la racha se rompió
                if (fechas.first() != hoy && fechas.first() != ayer) {
                    _rachaActual.value = 0
                    return@launch
                }

                // Contamos los días consecutivos hacia atrás
                var racha = 0
                var diaEsperado = fechas.first()

                for (fecha in fechas) {
                    if (fecha == diaEsperado) {
                        racha++
                        val cal = Calendar.getInstance().apply {
                            time = diaEsperado
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        diaEsperado = cal.time
                    } else {
                        break
                    }
                }

                // ¡Actualizamos el valor final!
                _rachaActual.value = racha

            } catch (e: Exception) {
                Log.e("Racha", "Error al calcular racha: ${e.message}")
            }
        }
    }
    // --- Funciones para Actualizar Campos Editables desde la UI ---
    fun onNombreChange(newName: String) { _editableNombre.value = newName }
    fun onNivelChange(nuevoNivel: String) { _editableNivel.value = nuevoNivel }
    fun onObjetivoSelected(objetivo: String, isSelected: Boolean) {
        val currentObjetivos = _editableObjetivos.value.toMutableList()
        if (isSelected) {
            if (!currentObjetivos.contains(objetivo)) {
                currentObjetivos.add(objetivo)
            }
        } else {
            currentObjetivos.remove(objetivo)
        }
        _editableObjetivos.value = currentObjetivos.toList() // Guardar como lista inmutable
    }
    fun onPesoChange(newPeso: String) { _editablePeso.value = newPeso }
    fun onAlturaChange(newAltura: String) { _editableAltura.value = newAltura }
    fun onEdadChange(newEdad: String) { _editableEdad.value = newEdad }
    fun onSexoChange(newSexo: String) { _editableSexo.value = newSexo }
    fun onFrecuenciaChange(newFrecuencia: String) { _editableFrecuenciaSemanal.value = newFrecuencia }
    fun onLugarEntrenamientoSelected(lugar: String, isSelected: Boolean) {
        val currentLugares = _editableLugarEntrenamiento.value.toMutableList()
        if (isSelected) {
            if (!currentLugares.contains(lugar)) {
                currentLugares.add(lugar)
            }
        } else {
            currentLugares.remove(lugar)
        }
        _editableLugarEntrenamiento.value = currentLugares.toList() // Guardar como lista inmutable
    }    // --- Carga y Gestión del Perfil del Usuario ---
    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false
            _user.value = null
            clearEditableFields()
            _homeScreenSummary.value = null
            _lastActivity.value = LastActivityItem.None
            _recommendedRoutines.value = emptyList()
            _userCustomRoutines.value = emptyList()
            _planSemanal.value = null
            _rutinaDeHoy.value = null
            return
        }
        _isLoadingUser.value = true
        _userErrorMessage.value = null
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val userProfile = doc.toObject(UserProfile::class.java)
                    _user.value = userProfile
                    populateEditableFields(userProfile)
                    loadRecommendedRoutines(userProfile)
                    loadUserCustomRoutines()
                    loadHomeScreenData()
                    calcularRachaActual(uid)
                    userProfile?.let { loadPlanSemanalActual(forceRegenerate = false) } // Modificado
                } else {
                    Log.w(TAG, "El documento del usuario no existe para UID: $uid")
                    _user.value = null
                    _userErrorMessage.value = "No se encontró el perfil del usuario."
                    clearEditableFields()
                    _homeScreenSummary.value = null
                    _lastActivity.value = LastActivityItem.None
                    _recommendedRoutines.value = emptyList()
                    _userCustomRoutines.value = emptyList()
                    _planSemanal.value = null
                    _rutinaDeHoy.value = null
                }
                _isLoadingUser.value = false
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar el perfil de usuario", exception)
                _user.value = null
                _isLoadingUser.value = false
                _userErrorMessage.value = "Error al cargar el perfil: ${exception.localizedMessage}"
                clearEditableFields()
                _homeScreenSummary.value = null
                _lastActivity.value = LastActivityItem.None
                _recommendedRoutines.value = emptyList()
                _userCustomRoutines.value = emptyList()
                _planSemanal.value = null
                _rutinaDeHoy.value = null
            }
    }
    // Modificamos loadPlanSemanalActual para aceptar un parámetro forceRegenerate
    fun loadPlanSemanalActual(forceRegenerate: Boolean = false) {
        val uid = firebaseAuth.currentUser?.uid
        val currentUserProfile = _user.value
        if (uid == null || currentUserProfile == null) {
            _planSemanalErrorMessage.value = "Usuario o perfil no disponible para cargar el plan semanal."
            _isLoadingPlanSemanal.value = false
            _planSemanal.value = null
            _rutinaDeHoy.value = null
            return
        }

        _isLoadingPlanSemanal.value = true
        _planSemanalErrorMessage.value = null
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                val planSemanalDocId = "semana_${year}_${weekOfYear}"

                if (forceRegenerate) {
                    Log.d(TAG, "Forzando regeneración del plan semanal para $planSemanalDocId.")
                    generarNuevoPlanSemanal(uid, currentUserProfile, planSemanalDocId)
                } else {
                    val planDoc = firestore.collection("users").document(uid)
                        .collection("planesSemanales")
                        .document(planSemanalDocId)
                        .get()
                        .await()

                    if (planDoc.exists()) {
                        val plan = planDoc.toObject(PlanSemanalUsuario::class.java)
                        _planSemanal.value = plan
                        determinarRutinaDeHoy()
                    } else {
                        Log.d(TAG, "No se encontró plan para $planSemanalDocId, generando uno nuevo.")
                        generarNuevoPlanSemanal(uid, currentUserProfile, planSemanalDocId)
                    }
                }
                _planSemanalErrorMessage.value = null // Limpiar error si todo fue bien
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la carga/generación del plan semanal", e)
                _planSemanalErrorMessage.value = "Error con plan: ${e.localizedMessage}"
                _planSemanal.value = null
                _rutinaDeHoy.value = null
            } finally {
                _isLoadingPlanSemanal.value = false
            }
        }
    }
    // NUEVA FUNCIÓN PÚBLICA PARA REGENERAR EL PLAN
    fun regenerateWeeklyPlan() {
        Log.d(TAG, "regenerateWeeklyPlan() llamado")
        // Simplemente llamamos a loadPlanSemanalActual con forceRegenerate = true
        // Esto reutilizará la lógica existente y los estados de carga/error.
        loadPlanSemanalActual(forceRegenerate = true)
    }
    private suspend fun generarNuevoPlanSemanal(userId: String, perfil: UserProfile, planSemanalDocId: String) {
        try {
            val calendar = Calendar.getInstance() // Obtiene la fecha y hora actual

            // 1. Ajustar 'calendar' para que apunte al LUNES de la semana actual.
            // Los días de la semana en Calendar van de SUNDAY (1) a SATURDAY (7).
            // MONDAY es 2.
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            var daysToSubtract = currentDayOfWeek - Calendar.MONDAY
            if (daysToSubtract < 0) {
                // Esto sucede si currentDayOfWeek es SUNDAY (1).
                // Calendar.MONDAY es 2, entonces 1 - 2 = -1.
                // Si hoy es Domingo, el Lunes de esta semana (considerando Lun-Dom) fue hace 6 días.
                daysToSubtract += 7 // Ajusta para retroceder correctamente desde Domingo al Lunes anterior.
            }
            calendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract)

            // 2. Normalizar a medianoche para el inicio de la semana (Lunes)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val fechaInicioSemana = Timestamp(calendar.time) // Este es el LUNES a las 00:00:00.000

            // 3. Calcular el fin de la semana (Domingo)
            // Creamos una nueva instancia para no modificar 'calendar' que ya es Lunes
            val finSemanaCal = Calendar.getInstance()
            finSemanaCal.time = fechaInicioSemana.toDate() // Empezar desde el Lunes
            finSemanaCal.add(Calendar.DAY_OF_YEAR, 6)    // Lunes + 6 días = Domingo

            // Normalizar a fin del día para el fin de la semana (Domingo)
            finSemanaCal.set(Calendar.HOUR_OF_DAY, 23)
            finSemanaCal.set(Calendar.MINUTE, 59)
            finSemanaCal.set(Calendar.SECOND, 59)
            finSemanaCal.set(Calendar.MILLISECOND, 999)
            val fechaFinSemana = Timestamp(finSemanaCal.time) // Este es el DOMINGO a las 23:59:59.999

            val diasPlanificados = mutableListOf<DiaDeEntrenamientoPlanificado>()
            val frecuencia = perfil.frecuenciaSemanal.coerceIn(0, 7)

            // Usamos una copia de 'calendar' (que está en Lunes) para iterar
            val tempCal = Calendar.getInstance()
            tempCal.time = fechaInicioSemana.toDate() // Empezar desde el inicio de la semana (Lunes)

            var diasEntrenamientoAsignados = 0
            // Definir los días laborables según la numeración de Calendar
            val diasLaborablesPreferidos = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )

            // Usar un Locale específico (por ejemplo, español) para los nombres de los días si quieres consistencia
            // o usa Locale.getDefault() si quieres que se adapte al idioma del dispositivo.
            // Para consistencia en la base de datos, un Locale fijo puede ser mejor.
            val sdfDia = SimpleDateFormat("EEEE", Locale("es", "ES")) // Ejemplo: Nombres en español

            for (i in 0..6) { // Iterar 7 días (Lunes a Domingo)
                val diaFecha = Timestamp(tempCal.time)
                // Usar el Locale especificado para formatear el nombre del día
                val nombreDia = sdfDia.format(tempCal.time).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
                }.uppercase(Locale("es", "ES")) // Asegurar mayúsculas y capitalización

                var esDiaDeEntrenamiento = false
                if (diasEntrenamientoAsignados < frecuencia) {
                    // Priorizar días laborables (Mon-Fri)
                    if (diasLaborablesPreferidos.contains(tempCal.get(Calendar.DAY_OF_WEEK))) {
                        esDiaDeEntrenamiento = true
                    }
                }
                // Si aún no hemos asignado suficientes y quedan días (incluyendo fines de semana)
                if (!esDiaDeEntrenamiento && diasEntrenamientoAsignados < frecuencia) {
                    // Asigna a cualquier día restante hasta cumplir frecuencia
                    // (Esto podría asignar a Sábado o Domingo si los laborables ya se llenaron
                    // o si la frecuencia es alta)
                    esDiaDeEntrenamiento = true
                }

                if (esDiaDeEntrenamiento) {
                    diasPlanificados.add(
                        DiaDeEntrenamientoPlanificado(
                            fecha = diaFecha,
                            diaDeLaSemana = nombreDia,
                            tipoDeDia = TipoDiaEntrenamiento.ENTRENAMIENTO.name,
                            completada = false
                        )
                    )
                    diasEntrenamientoAsignados++
                } else {
                    diasPlanificados.add(
                        DiaDeEntrenamientoPlanificado(
                            fecha = diaFecha,
                            diaDeLaSemana = nombreDia,
                            tipoDeDia = TipoDiaEntrenamiento.DESCANSO.name,
                            completada = false
                        )
                    )
                }
                tempCal.add(Calendar.DAY_OF_MONTH, 1) // Avanzar al siguiente día
            }

            val nuevoPlan = PlanSemanalUsuario(
                id = planSemanalDocId,
                userId = userId,
                fechaInicioSemana = fechaInicioSemana, // Lunes
                fechaFinSemana = fechaFinSemana,     // Domingo
                diasPlanificados = diasPlanificados.toMutableList(),
                frecuenciaObjetivoOriginal = perfil.frecuenciaSemanal,
                ultimaActualizacion = Timestamp.now()
            )

            firestore.collection("users").document(userId)
                .collection("planesSemanales")
                .document(planSemanalDocId)
                .set(nuevoPlan)
                .await()

            _planSemanal.value = nuevoPlan
            determinarRutinaDeHoy() // Esto debería seguir funcionando bien
            Log.d(TAG, "Nuevo plan semanal (Lunes-Domingo) generado y guardado para $planSemanalDocId")

        } catch (e: Exception) {
            Log.e(TAG, "Error al generar nuevo plan semanal", e)
            _planSemanalErrorMessage.value = "Error al generar plan: ${e.localizedMessage}"
            throw e // Relanzar para que el llamador (loadPlanSemanalActual) lo maneje
        }
        // El 'finally' para _isLoadingPlanSemanal está en la función llamadora (loadPlanSemanalActual)
    }
    private fun determinarRutinaDeHoy() {
        val planActual = _planSemanal.value
        if (planActual == null) {
            _rutinaDeHoy.value = null
            return
        }

        val calendarHoy = Calendar.getInstance()
        // Normalizar 'hoy' a medianoche para comparar solo fechas
        calendarHoy.set(Calendar.HOUR_OF_DAY, 0)
        calendarHoy.set(Calendar.MINUTE, 0)
        calendarHoy.set(Calendar.SECOND, 0)
        calendarHoy.set(Calendar.MILLISECOND, 0)
        val hoyTimestampNormalized = Timestamp(calendarHoy.time)

        val diaDeHoyPlanificado = planActual.diasPlanificados.find { diaPlan ->
            val calPlan = Calendar.getInstance()
            calPlan.time = diaPlan.fecha.toDate()
            calPlan.set(Calendar.HOUR_OF_DAY, 0)
            calPlan.set(Calendar.MINUTE, 0)
            calPlan.set(Calendar.SECOND, 0)
            calPlan.set(Calendar.MILLISECOND, 0)
            val planTimestampNormalized = Timestamp(calPlan.time)
            planTimestampNormalized == hoyTimestampNormalized
        }

        _rutinaDeHoy.value = diaDeHoyPlanificado
        Log.d(TAG, "Rutina de hoy determinada: ${diaDeHoyPlanificado?.nombreRutinaAsignada ?: diaDeHoyPlanificado?.tipoRutina ?: "Ninguna"}")
    }
    fun updateDayInWeeklyPlan(
        dateToUpdate: Date, // La fecha exacta del día a actualizar
        rutinaId: String?,
        rutinaNombre: String?,
        esCustom: Boolean?,
        tipoDeDia: String // Ej: TipoDiaEntrenamiento.ENTRENAMIENTO.name o TipoDiaEntrenamiento.DESCANSO.name
    ) {
        val currentUser = _user.value ?: run {
            Log.w(TAG, "updateDayInWeeklyPlan: Usuario no disponible.")
            _planSemanalErrorMessage.value = "Usuario no disponible para actualizar el plan."
            return
        }
        val currentPlan = _planSemanal.value ?: run {
            Log.w(TAG, "updateDayInWeeklyPlan: Plan semanal no disponible.")
            _planSemanalErrorMessage.value = "Plan semanal no disponible para actualizar."
            return
        }
        val uid = currentUser.uid

        viewModelScope.launch {
            _isLoadingPlanSemanal.value = true
            try {
                // CORRECCIÓN: Usar 'diasPlanificados' en lugar de 'diasDelPlan'
                val updatedDiasPlanificados = currentPlan.diasPlanificados.map { dia ->
                    // Compara solo día, mes y año para la fecha
                    val calDia = Calendar.getInstance().apply { time = dia.fecha.toDate() }
                    val calDateToUpdate = Calendar.getInstance().apply { time = dateToUpdate }

                    if (calDia.get(Calendar.DAY_OF_YEAR) == calDateToUpdate.get(Calendar.DAY_OF_YEAR) &&
                        calDia.get(Calendar.YEAR) == calDateToUpdate.get(Calendar.YEAR)
                    ) {
                        // La función .copy() aquí es la generada para la data class DiaDeEntrenamientoPlanificado
                        dia.copy(
                            rutinaIdAsignada = rutinaId,
                            nombreRutinaAsignada = rutinaNombre,
                            esRutinaPersonalizada = esCustom, // Ya es Boolean?, no necesita ?: false aquí a menos que el modelo lo requiera no nulo
                            tipoDeDia = tipoDeDia,
                            // Si se cambia a día de DESCANSO o se quita la rutina, se resetea 'completada' y 'progresoRutinaIdCompletada'
                            completada = if (tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name || rutinaId == null) false else dia.completada,
                            progresoRutinaIdCompletada = if (tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name || rutinaId == null) null else dia.progresoRutinaIdCompletada
                        )
                    } else {
                        dia
                    }
                }

                // CORRECCIÓN: Usar 'diasPlanificados' al crear la copia del plan
                // Asegúrate de que PlanSemanalUsuario.diasPlanificados sea MutableList si quieres modificarlo directamente
                // o que la copia cree una nueva lista mutable si es necesario.
                // Si tu modelo PlanSemanalUsuario.diasPlanificados es List y no MutableList, toList() está bien.
                // Si es MutableList, .toMutableList() es redundante si updatedDiasPlanificados ya es una nueva lista,
                // pero no dañino. Si es MutableList y quieres modificar la instancia original (no recomendado aquí),
                // la lógica sería diferente.
                val updatedPlan = currentPlan.copy(diasPlanificados = updatedDiasPlanificados.toMutableList()) // Asumiendo que PlanSemanalUsuario.diasPlanificados es MutableList<DiaDeEntrenamientoPlanificado>

                firestore.collection("users").document(uid)
                    .collection("planesSemanales").document(currentPlan.id)
                    .set(updatedPlan) // .set() sobrescribe el documento. También puedes usar .update()
                    .await()

                _planSemanal.value = updatedPlan
                Log.d(TAG, "Plan semanal actualizado para el día: $dateToUpdate con rutina: $rutinaNombre")
                determinarRutinaDeHoy() // Re-evaluar la rutina de hoy
                _planSemanalErrorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar día en plan semanal", e)
                _planSemanalErrorMessage.value = "Error al actualizar plan: ${e.localizedMessage ?: e.message}"
                // Considera no cambiar _planSemanal.value aquí para mantener el estado anterior
                // o revertirlo al 'currentPlan' original si la UI lo necesita.
            } finally {
                _isLoadingPlanSemanal.value = false
            }
        }
    }
    fun marcarDiaComoCompletado(fechaDiaCompletado: Timestamp, progresoRutinaId: String, rutinaNombre: String?, esCustom: Boolean?) {
        val uid = firebaseAuth.currentUser?.uid
        val currentPlan = _planSemanal.value
        if (uid == null || currentPlan == null) {
            Log.e(TAG, "No se puede marcar como completado: usuario o plan no disponible.")
            _planSemanalErrorMessage.value = "No se puede marcar como completado: información faltante."
            return
        }

        viewModelScope.launch {
            _isLoadingPlanSemanal.value = true // Indicar que estamos modificando el plan
            try {
                val calendarCompletado = Calendar.getInstance()
                calendarCompletado.time = fechaDiaCompletado.toDate()
                calendarCompletado.set(Calendar.HOUR_OF_DAY, 0)
                calendarCompletado.set(Calendar.MINUTE, 0)
                calendarCompletado.set(Calendar.SECOND, 0)
                calendarCompletado.set(Calendar.MILLISECOND, 0)
                val completadoTimestampNormalized = Timestamp(calendarCompletado.time)


                // CORRECCIÓN: Usar 'diasPlanificados'
                val updatedDiasPlanificados = currentPlan.diasPlanificados.map { dia ->
                    val calPlan = Calendar.getInstance()
                    calPlan.time = dia.fecha.toDate()
                    calPlan.set(Calendar.HOUR_OF_DAY, 0)
                    calPlan.set(Calendar.MINUTE, 0)
                    calPlan.set(Calendar.SECOND, 0)
                    calPlan.set(Calendar.MILLISECOND, 0)
                    val planTimestampNormalized = Timestamp(calPlan.time)

                    if (planTimestampNormalized == completadoTimestampNormalized) {
                        dia.copy(
                            completada = true,
                            progresoRutinaIdCompletada = progresoRutinaId,
                            // Si la rutina se seleccionó al momento de completar y no estaba preasignada,
                            // actualizamos también la información de la rutina asignada.
                            rutinaIdAsignada = dia.rutinaIdAsignada ?: progresoRutinaId, // Asumimos que progresoRutinaId es el ID de la Rutina o UserCustomRoutine
                            nombreRutinaAsignada = rutinaNombre ?: dia.nombreRutinaAsignada,
                            esRutinaPersonalizada = esCustom ?: dia.esRutinaPersonalizada,
                            tipoDeDia = TipoDiaEntrenamiento.ENTRENAMIENTO.name // Marcar como entrenamiento si se completó una rutina
                        )
                    } else {
                        dia
                    }
                }

                // CORRECCIÓN: Usar 'diasPlanificados'
                val updatedPlan = currentPlan.copy(diasPlanificados = updatedDiasPlanificados.toMutableList()) // Asumiendo que PlanSemanalUsuario.diasPlanificados es MutableList

                firestore.collection("users").document(uid)
                    .collection("planesSemanales").document(currentPlan.id)
                    .set(updatedPlan)
                    .await()

                _planSemanal.value = updatedPlan
                determinarRutinaDeHoy()

                // Actualizar ResumenSemanal ya que esta acción impacta las métricas
                loadHomeScreenData()
                _planSemanalErrorMessage.value = null

            } catch (e: Exception) {
                Log.e(TAG, "Error al marcar día como completado", e)
                _planSemanalErrorMessage.value = "Error al marcar completado: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoadingPlanSemanal.value = false
            }
        }
    }
    private fun populateEditableFields(userProfile: UserProfile?) {
        userProfile?.let {
            _editableNombre.value = it.nombre
            _editableNivel.value = it.nivel
            _editableObjetivos.value = it.objetivos
            _editablePeso.value = it.peso.takeIf { p -> p > 0f }?.toString() ?: ""
            _editableAltura.value = it.altura.takeIf { a -> a > 0f }?.toString() ?: ""
            _editableEdad.value = it.edad.takeIf { e -> e > 0 }?.toString() ?: ""
            _editableSexo.value = it.sexo
            _editableFrecuenciaSemanal.value = it.frecuenciaSemanal.takeIf { f -> f >= 0 }?.toString() ?: "3" // 0 puede ser válido
            _editableLugarEntrenamiento.value = it.lugarEntrenamiento
        }
    }
    private fun clearEditableFields() {
        _editableNombre.value = ""
        _editableNivel.value = ""
        _editableObjetivos.value = emptyList()
        _editablePeso.value = ""
        _editableAltura.value = ""
        _editableEdad.value = ""
        _editableSexo.value = ""
        _editableFrecuenciaSemanal.value = ""
        _editableLugarEntrenamiento.value = emptyList()
    }
    fun saveUserProfile(newImageUri: Uri?) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _updateState.value = UpdateProfileState.Error("Usuario no autenticado.")
            return
        }

        _updateState.value = UpdateProfileState.Loading
        viewModelScope.launch {
            try {
                var finalImageUrl = _user.value?.fotoUrl ?: ""

                if (newImageUri != null) {
                    val imageFileName = "${System.currentTimeMillis()}.jpg"
                    val storageRef = storage.reference.child("fotos_perfil/$currentUserId/$imageFileName")
                    storageRef.putFile(newImageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }
                val updatedUserProfileData = UserProfile(
                    uid = currentUserId, // Importante incluir el uid si es un objeto nuevo
                    nombre = _editableNombre.value,
                    email = _user.value?.email ?: "", // El email no se edita aquí, tomar el existente
                    fechaRegistro = _user.value?.fechaRegistro, // La fecha de registro no cambia

                    // --- NUEVO: Guardar Nivel ---
                    nivel = _editableNivel.value,
                    // --- NUEVO: Guardar Objetivos ---
                    objetivos = _editableObjetivos.value,

                    peso = _editablePeso.value.toFloatOrNull() ?: _user.value?.peso ?: 0f,
                    altura = _editableAltura.value.toFloatOrNull() ?: _user.value?.altura ?: 0f,
                    edad = _editableEdad.value.toIntOrNull() ?: _user.value?.edad ?: 0,
                    sexo = _editableSexo.value,
                    frecuenciaSemanal = _editableFrecuenciaSemanal.value.toIntOrNull() ?: _user.value?.frecuenciaSemanal ?: 3,

                    // --- MODIFICADO: Guardar Lugar de Entrenamiento ---
                    lugarEntrenamiento = _editableLugarEntrenamiento.value, // Ya es una lista

                    insignias = _user.value?.insignias ?: emptyList(), // No se editan aquí
                    rutinasCompletadas = _user.value?.rutinasCompletadas ?: 0, // No se editan aquí
                    progresoActual = _user.value?.progresoActual ?: "", // No se edita aquí
                    fotoUrl = finalImageUrl
                )
                firestore.collection("users").document(currentUserId)
                    .set(updatedUserProfileData)
                    .await()

                loadUserProfile() // Recargar para reflejar cambios
                _updateState.value = UpdateProfileState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar el perfil del usuario", e)
                _updateState.value = UpdateProfileState.Error(e.message ?: "Error desconocido al guardar.")
            }
        }
    }
    fun resetUpdateState() {
        _updateState.value = UpdateProfileState.Idle
    }
    // --- Carga de Datos para HomeScreen ---
    fun loadHomeScreenData() {
        val uid = firebaseAuth.currentUser?.uid
        val currentUserProfile = _user.value // Obtener el perfil actual

        if (uid == null) {
            _homeScreenErrorMessage.value = "Usuario no autenticado para datos del home."
            _isLoadingHomeScreenData.value = false
            _lastActivity.value = LastActivityItem.None
            _homeScreenSummary.value = null
            return
        }

        // Si el perfil aún no está cargado, es posible que loadUserProfile aún no haya terminado
        // o falló. En un escenario ideal, esta función se llamaría solo después de una carga exitosa del perfil.
        // Por ahora, si no está, mostraremos un mensaje y no procederemos con todos los cálculos.
        if (currentUserProfile == null) {
            _homeScreenErrorMessage.value = "Perfil de usuario no disponible para calcular resumen semanal."
            _isLoadingHomeScreenData.value = true // Aún intentando (puede que el perfil cargue pronto)
            // No reseteamos _lastActivity aquí, podría estar cargando de una llamada anterior
            // o ya tener un valor. Si quieres un reset más agresivo:
            // _lastActivity.value = LastActivityItem.Loading
            // _homeScreenSummary.value = null
            // Considera no continuar si el perfil es esencial para TODOS los datos del HomeScreen.
            // Opcionalmente, podrías intentar cargar solo la última actividad si no depende del perfil.
            // Para este ejemplo, seremos estrictos: si no hay perfil, no hay resumen nuevo.
            // La llamada desde loadUserProfile() debería mitigar esto.
            // Si init llama a loadHomeScreenData antes que loadUserProfile termine, _user.value será null.
            // Por eso la llamada en loadUserProfile es importante.

            // Si llegamos aquí DESPUÉS de que loadUserProfile falló, _isLoadingUser será false.
            // Si loadUserProfile está en progreso, _isLoadingUser será true.
            // Si queremos ser menos estrictos y cargar lo que se pueda:
            // viewModelScope.launch { loadLastActivityOnly(uid) } // Función hipotética
            return // Salir si no hay perfil para el resumen
        }

        _isLoadingHomeScreenData.value = true
        _homeScreenErrorMessage.value = null
        _lastActivity.value = LastActivityItem.Loading

        viewModelScope.launch {
            try {
                coroutineScope {
                    // 1. Cargar Resumen Semanal de Rutinas (tarea asíncrona)
                    val resumenAsync = async {
                        // --- INICIO: CÁLCULO DEL INICIO DE LA SEMANA (EJ. LUNES) ---
                        val calendarInicioSemana = Calendar.getInstance()
                        // Normalizar a hoy a las 00:00:00
                        calendarInicioSemana.set(Calendar.HOUR_OF_DAY, 0)
                        calendarInicioSemana.set(Calendar.MINUTE, 0)
                        calendarInicioSemana.set(Calendar.SECOND, 0)
                        calendarInicioSemana.set(Calendar.MILLISECOND, 0)

                        // Ajustar al primer día de la semana (LUNES)
                        // Calendar.MONDAY es 2. Calendar.SUNDAY es 1.
                        val primerDiaDeLaSemana = Calendar.MONDAY // Puedes hacerlo configurable si es necesario
                        var diasARetroceder = calendarInicioSemana.get(Calendar.DAY_OF_WEEK) - primerDiaDeLaSemana
                        if (diasARetroceder < 0) {
                            diasARetroceder += 7 // Si hoy es Domingo y la semana empieza Lunes, retrocede 6 días
                        }
                        calendarInicioSemana.add(Calendar.DAY_OF_YEAR, -diasARetroceder)
                        val inicioSemanaRealTimestamp = Timestamp(calendarInicioSemana.time)
                        // --- FIN: CÁLCULO DEL INICIO DE LA SEMANA ---

                        Log.d(TAG, "Calculando resumen semanal desde: ${inicioSemanaRealTimestamp.toDate()}")

                        val rutinasQuerySnapshot = firestore.collection("users").document(uid)
                            .collection("progresoRutinas")
                            // Usar el inicio de semana real calculado
                            .whereGreaterThanOrEqualTo("fecha", inicioSemanaRealTimestamp)
                            .get()
                            .await()

                        val rutinasDeEstaSemana = rutinasQuerySnapshot.toObjects(ProgresoRutina::class.java)

                        // Filtrar adicionalmente si la consulta pudiera traer datos de la "próxima" semana
                        // (poco probable con >= inicioSemanaRealTimestamp, pero por seguridad si las fechas son futuras)
                        // val hoyFinDia = Calendar.getInstance()
                        // hoyFinDia.set(Calendar.HOUR_OF_DAY, 23)
                        // hoyFinDia.set(Calendar.MINUTE, 59)
                        // hoyFinDia.set(Calendar.SECOND, 59)
                        // val finHoyTimestamp = Timestamp(hoyFinDia.time)
                        // val rutinasFiltradas = rutinasDeEstaSemana.filter { it.fecha <= finHoyTimestamp }
                        // Por ahora, asumimos que "progresoRutinas" no tendrá fechas futuras.

                        val tiempoTotalSemanasSegundos = rutinasDeEstaSemana.sumOf { it.tiempoTotalSesionSegundos }

                        val diasActivos = rutinasDeEstaSemana
                            .map { progreso ->
                                val cal = Calendar.getInstance()
                                cal.time = progreso.fecha.toDate()
                                cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                            }
                            .distinct()
                            .count()

                        ResumenSemanal(
                            rutinasCompletadasEstaSemana = rutinasDeEstaSemana.size,
                            tiempoTotalEntrenadoSegundosEstaSemana = tiempoTotalSemanasSegundos,
                            frecuenciaSemanalObjetivo = currentUserProfile.frecuenciaSemanal,
                            diasActivosEstaSemana = diasActivos,
                            objetivosCompletados = _homeScreenSummary.value?.objetivosCompletados ?: emptyList(),
                            insigniasObtenidas = _homeScreenSummary.value?.insigniasObtenidas ?: emptyList(),
                            progresoActual = currentUserProfile.progresoActual
                        )
                    }
                    // 2. Cargar Última Rutina (tarea asíncrona)
                    val ultimaRutinaAsync = async {
                        val ultimaRutinaQuery = firestore.collection("users").document(uid)
                            .collection("progresoRutinas")
                            // .whereEqualTo("userId", uid) // Ver comentario anterior
                            .orderBy("fecha", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .await()
                        ultimaRutinaQuery.documents.firstOrNull()?.toObject(ProgresoRutina::class.java)
                    }

                    // 3. Cargar Última Actividad Libre (tarea asíncrona)
                    val ultimaActividadLibreAsync = async {
                        val coleccionActividades = "activities" // O "userActivities", según tu estructura
                        val ultimaActividadLibreQuery = firestore.collection("users").document(uid)
                            .collection(coleccionActividades)
                            // .whereEqualTo("userId", uid) // Ver comentario anterior
                            .orderBy("timestamp", Query.Direction.DESCENDING) // Asumiendo que UserActivity.timestamp es el campo de fecha
                            .limit(1)
                            .get()
                            .await()
                        ultimaActividadLibreQuery.documents.firstOrNull()?.toObject(UserActivity::class.java)
                    }

                    // Esperar resultados de todas las tareas asíncronas
                    val resumenSemanalCalculado = resumenAsync.await()
                    val ultimaRutina = ultimaRutinaAsync.await()
                    val ultimaActividadLibre = ultimaActividadLibreAsync.await()

                    // Actualizar los StateFlows con los resultados
                    _homeScreenSummary.value = resumenSemanalCalculado

                    // Determinar cuál es la más reciente (lógica existente)
                    when {
                        ultimaRutina == null && ultimaActividadLibre == null -> {
                            _lastActivity.value = LastActivityItem.None
                        }
                        ultimaRutina != null && ultimaActividadLibre == null -> {
                            _lastActivity.value = LastActivityItem.Routine(ultimaRutina)
                        }
                        ultimaRutina == null && ultimaActividadLibre != null -> {
                            _lastActivity.value = LastActivityItem.FreeActivity(ultimaActividadLibre)
                        }
                        ultimaRutina != null && ultimaActividadLibre != null -> {
                            val actividadLibreTimestamp = ultimaActividadLibre.timestamp?.let { Timestamp(it) }
                            if (actividadLibreTimestamp == null || ultimaRutina.fecha > actividadLibreTimestamp) {
                                _lastActivity.value = LastActivityItem.Routine(ultimaRutina)
                            } else {
                                _lastActivity.value = LastActivityItem.FreeActivity(ultimaActividadLibre)
                            }
                        }
                    }
                    _homeScreenErrorMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos del home screen", e)
                _homeScreenErrorMessage.value = "Error al cargar datos: ${e.localizedMessage}"
                // No necesariamente reseteamos el resumen aquí, podría mostrar el último válido
                // _homeScreenSummary.value = null
                _lastActivity.value = LastActivityItem.None // Indicar error o no actividad
            } finally {
                _isLoadingHomeScreenData.value = false
            }
        }
    }
    fun refreshHomeScreenData() {
        loadHomeScreenData()
    }
    // --- Carga de Rutinas Recomendadas ---
    fun loadRecommendedRoutines(currentUserProfile: UserProfile?) {
        val profileToUse = currentUserProfile ?: _user.value
        if (profileToUse == null || profileToUse.uid.isBlank()) { // uid check for safety
            _routinesErrorMessage.value = "Perfil de usuario no disponible."
            _isLoadingRoutines.value = false
            _recommendedRoutines.value = emptyList()
            return
        }

        _isLoadingRoutines.value = true
        _routinesErrorMessage.value = null
        _recommendedRoutines.value = emptyList()

        viewModelScope.launch {
            try {
                // Convertir lugares de entrenamiento del perfil (Strings) a Enums
                val userLocationEnums: List<LugarEntrenamiento> = profileToUse.lugarEntrenamiento
                    .mapNotNull { lugarString ->
                        try {
                            LugarEntrenamiento.entries.firstOrNull { it.name.equals(lugarString.trim(), ignoreCase = true) }
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Lugar de entrenamiento del usuario '$lugarString' no es un enum válido.")
                            null
                        }
                    }

                // Llamar a la función que obtiene las rutinas (idealmente en un Repositorio)
                // Esta es una función de ejemplo, su implementación real está fuera de este ViewModel
                val rutinasList = obtenerRutinasDesdeFirestore(
                    nivel = profileToUse.nivel.takeIf { it.isNotBlank() },
                    objetivos = profileToUse.objetivos.takeIf { it.isNotEmpty() },
                    lugaresEntrenamientoEnums = userLocationEnums.takeIf { it.isNotEmpty() }
                )

                _recommendedRoutines.value = rutinasList.take(5) // Tomar un número limitado
                if (rutinasList.isEmpty()) {
                    _routinesErrorMessage.value = "No se encontraron rutinas con tus preferencias."
                } else {
                    _routinesErrorMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar rutinas recomendadas", e)
                _recommendedRoutines.value = emptyList()
                _routinesErrorMessage.value = "Error al cargar recomendaciones: ${e.localizedMessage}"
            } finally {
                _isLoadingRoutines.value = false
            }
        }
    }
    // Esta función debería idealmente estar en una clase Repositorio.
    // Aquí se incluye como ejemplo de cómo podría ser la lógica de consulta.
    private suspend fun obtenerRutinasDesdeFirestore(
        nivel: String?,
        objetivos: List<String>?,
        lugaresEntrenamientoEnums: List<LugarEntrenamiento>?
    ): List<Rutina> {
        try {
            var query: Query = firestore.collection("rutinas")
            var appliedServerFilter = false

            // Aplicar el filtro de NIVEL en el servidor si está presente
            if (nivel != null) {
                query = query.whereArrayContains("nivelRecomendado", nivel)
                appliedServerFilter = true
            }
            // Si no se filtró por nivel, intentar filtrar por OBJETIVOS en el servidor
            else if (objetivos != null && objetivos.isNotEmpty()) {
                query = query.whereArrayContainsAny("objetivos", objetivos.take(10)) // Firestore limita a 10 en 'array-contains-any'
                appliedServerFilter = true
            }
            // Si no se filtró por nivel ni objetivos, intentar filtrar por LUGAR en el servidor
            else if (lugaresEntrenamientoEnums != null && lugaresEntrenamientoEnums.isNotEmpty()) {
                val lugaresStr = lugaresEntrenamientoEnums.map { it.name }
                query = query.whereArrayContainsAny("lugarEntrenamiento", lugaresStr.take(10)) // Firestore limita a 10
                appliedServerFilter = true
            }

            val snapshot = query.limit(50).get().await() // Obtener un conjunto más grande si se filtra mucho en cliente
            var rutinasFetched = snapshot.toObjects(Rutina::class.java)

            // Filtrado en Cliente para los criterios NO aplicados en servidor
            if (nivel != null && appliedServerFilter && !query.toString().contains("nivelRecomendado")) { // Verificación torpe, mejorar
                rutinasFetched = rutinasFetched.filter { rutina -> rutina.nivelRecomendado.any { it.equals(nivel, ignoreCase = true) } }
            }
            // Si el filtro de nivel se aplicó en el servidor, ya no es necesario filtrarlo en cliente
            // a menos que la lógica del servidor haya sido "else if"

            // Filtrar por objetivos en cliente SI no se hizo en servidor O si se quiere una lógica más compleja (e.g., TODOS los objetivos)
            if (objetivos != null && objetivos.isNotEmpty()) {
                rutinasFetched = rutinasFetched.filter { rutina ->
                    // Ejemplo: si se requiere que la rutina tenga AL MENOS UNO de los objetivos del usuario
                    objetivos.any { objetivoUsuario -> rutina.objetivos.any { it.equals(objetivoUsuario, ignoreCase = true) } }
                    // O si se requiere que la rutina tenga TODOS los objetivos del usuario:
                    // objetivos.all { objetivoUsuario -> rutina.objetivos.any { it.equals(objetivoUsuario, ignoreCase = true) } }
                }
            }

            // Filtrar por lugar en cliente SI no se hizo en servidor
            if (lugaresEntrenamientoEnums != null && lugaresEntrenamientoEnums.isNotEmpty()) {
                val nombresLugaresUsuario = lugaresEntrenamientoEnums.map { it.name }
                rutinasFetched = rutinasFetched.filter { rutina ->
                    rutina.lugarEntrenamiento.any { lugarRutina -> // Asumiendo que rutina.lugarEntrenamiento es List<String>
                        nombresLugaresUsuario.any { it.equals(lugarRutina.toString(), ignoreCase = true) }
                    }
                }
            }

            return rutinasFetched
        } catch (e: Exception) {
            Log.e(TAG, "Error en obtenerRutinasDesdeFirestore: ${e.message}", e)
            throw e
        }
    }
    fun refreshRecommendations() {
        loadRecommendedRoutines(null) // Usa el perfil de usuario actual en _user.value
    }
    // --- Carga de Rutinas Personalizadas del Usuario --- // *** NUEVA SECCIÓN ***
    fun loadUserCustomRoutines() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _userCustomRoutinesError.value = "Usuario no autenticado."
            _isLoadingUserCustomRoutines.value = false
            _userCustomRoutines.value = emptyList()
            return
        }

        _isLoadingUserCustomRoutines.value = true
        _userCustomRoutinesError.value = null
        // _userCustomRoutines.value = emptyList() // Opcional: limpiar antes de cargar

        viewModelScope.launch {
            try {
                // *** ASUME que tienes una colección "userCustomRoutines" anidada bajo "users/{userId}/userCustomRoutines" ***
                // *** O si es una colección de nivel raíz, ajusta la ruta y añade .whereEqualTo("userId", uid) ***
                val querySnapshot = firestore.collection("users").document(uid)
                    .collection("customRoutines") // AJUSTA ESTA RUTA SI ES DIFERENTE
                    .orderBy("fechaCreacion", Query.Direction.DESCENDING) // Opcional: ordenar por fecha
                    .get()
                    .await()

                val routines = querySnapshot.toObjects(UserCustomRoutine::class.java)
                _userCustomRoutines.value = routines
                if (routines.isEmpty()) {
                    // No es necesariamente un error, podría simplemente no tener rutinas
                    // _userCustomRoutinesError.value = "No tienes rutinas personalizadas todavía."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar las rutinas personalizadas del usuario", e)
                _userCustomRoutines.value = emptyList()
                _userCustomRoutinesError.value = "Error al cargar tus rutinas: ${e.localizedMessage}"
            } finally {
                _isLoadingUserCustomRoutines.value = false
            }
        }
    }
    fun refreshUserCustomRoutines() { // Para pull-to-refresh
        loadUserCustomRoutines()
    }
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
    // Limpiar estados cuando el ViewModel se destruye (o el usuario se desloguea explícitamente)
    override fun onCleared() {
        super.onCleared()
        // Aquí podrías resetear estados si es necesario, aunque los StateFlows se manejarán
        // por el ciclo de vida de sus colectores.
        Log.d(TAG, "UserProfileViewModel onCleared")
    }
}