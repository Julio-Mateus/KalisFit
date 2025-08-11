package com.jcmateus.kalisfit.navigation


object Routes {

    // Objeto anidado para los NOMBRES DE LOS ARGUMENTOS
    // Esto centraliza los nombres de los argumentos para evitar errores de tipeo
    // y facilitar la refactorización.
    object Args {
        // Argumentos para RoutineDetailScreen
        const val ROUTINE_ID_ARG = "routineId"

        // Argumentos para CalisthenicsLevelDetailScreen
        const val PROGRESSION_ID_ARG = "progressionId"
        const val LEVEL_ID_ARG = "levelId"

        // Argumentos para ProductDetailScreen
        const val PRODUCT_ID_ARG = "productId"

        // Argumentos para EditRoutineScreen (los que estamos añadiendo ahora)
        const val USER_ID_ARG = "userId"
        const val TEMPLATE_ID_ARG = "templateId"
        const val CUSTOM_ROUTINE_ID_ARG = "customRoutineId"

        // Puedes añadir más argumentos aquí a medida que los necesites para otras rutas
    }

    // --- Rutas de Autenticación y Onboarding ---
    const val SPLASH = "splash_screen"
    const val LOGIN = "login_screen"
    const val REGISTER = "register_screen"
    const val FORGOT_PASSWORD = "forgot_password_screen"
    const val ONBOARDING = "onboarding_screen"
    const val ONBOARDING_SUCCESS = "onboarding_success_screen"

    // --- Contenedor Principal (con Drawer, TopAppBar y BottomNav) ---
    const val MAIN_CONTENT = "main_content_screen"

    // --- Pestañas del BottomNavigationBar ---
    const val HOME_TAB = "home_tab"
    const val CALISTHENICS_TAB = "calisthenics_tab"
    const val STOICISM_TAB = "stoicism_tab"
    const val RUNNING_TAB = "running_tab"
    const val STORE_TAB = "store_tab" // Movida aquí para agrupar con otras pestañas

    // --- Rutas de Nivel Superior (Sin BottomNav) ---
    const val PROFILE_SCREEN = "profile_screen"
    const val EDIT_PROFILE_SCREEN = "edit_profile_screen"
    const val SETTINGS_SCREEN = "settings_screen"
    const val ROUTINES_EXPLORER_SCREEN = "routines_explorer_screen"
    const val ACTIVITY_HISTORY_SCREEN = "activity_history_screen"
    const val TIPS_SCREEN = "tips_screen"
    const val CART_SCREEN = "cart_screen" // Movida para agrupar con otras de nivel superior

    // --- Rutas con Argumentos (Usando el objeto Args) ---

    // Rutina Detalle
    const val ROUTINE_DETAIL_PREFIX = "routine_detail"
    // Ahora usa la constante de Args para el nombre del argumento
    const val ROUTINE_DETAIL_SCREEN =
        "$ROUTINE_DETAIL_PREFIX/{${Args.ROUTINE_ID_ARG}}?${Args.USER_ID_ARG}={${Args.USER_ID_ARG}}"
    // Ya no necesitas: const val ROUTINE_DETAIL_ARG_ID = "routineId" porque está en Args
    const val ROUTINE_SUCCESS_SCREEN = "routine_success_screen"
    // --- NUEVA RUTA PARA LA EJECUCIÓN DE LA RUTINA (RoutineScreen) ---
    const val ROUTINE_EXECUTION_PREFIX = "routine_execution" // O simplemente "routine_screen"
    // Usaremos Args.ROUTINE_ID_ARG si el ID que espera RoutineScreen es el mismo que el de RoutineDetailScreen
    const val ROUTINE_EXECUTION_SCREEN = "$ROUTINE_EXECUTION_PREFIX/{${Args.ROUTINE_ID_ARG}}"
    // Calistenia Detalle Nivel
    const val CALISTHENICS_LEVEL_DETAIL_PREFIX = "calisthenics_level_detail"
    const val CALISTHENICS_LEVEL_DETAIL_SCREEN = "$CALISTHENICS_LEVEL_DETAIL_PREFIX/{${Args.PROGRESSION_ID_ARG}}/{${Args.LEVEL_ID_ARG}}"
    // --- Rutas de Nivel Superior (Sin BottomNav) ---
    const val MY_CUSTOM_ROUTINES_SCREEN = "my_custom_routines"
    const val ALL_EXERCISES_SCREEN = "all_exercises_screen"
    // Producto Detalle
    const val PRODUCT_DETAIL_BASE = "productDetail"
    const val PRODUCT_DETAIL_SCREEN = "$PRODUCT_DETAIL_BASE/{${Args.PRODUCT_ID_ARG}}"

    // Edición/Personalización de Rutina
    const val EDIT_ROUTINE_PREFIX = "edit_routine"
    const val EDIT_ROUTINE_SCREEN_BASE = EDIT_ROUTINE_PREFIX // Más corto y directo
    const val EDIT_ROUTINE_SCREEN_ROUTE_TEMPLATE =
        "$EDIT_ROUTINE_SCREEN_BASE" +
                "?${Args.USER_ID_ARG}={${Args.USER_ID_ARG}}" +
                "&${Args.TEMPLATE_ID_ARG}={${Args.TEMPLATE_ID_ARG}}" +
                "&${Args.CUSTOM_ROUTINE_ID_ARG}={${Args.CUSTOM_ROUTINE_ID_ARG}}"


    // --- FUNCIONES HELPER PARA CONSTRUIR RUTAS ---
    fun startRoutineExecution(routineId: String): String {
        // Asumiendo que ROUTINE_ID_ARG es el nombre del argumento que RoutineScreen espera
        return "$ROUTINE_EXECUTION_PREFIX/$routineId"
    }
    fun routineDetail(routineId: String, userId: String? = null): String {
        // userId es ahora un parámetro opcional en la función
        var route = "$ROUTINE_DETAIL_PREFIX/$routineId"
        if (userId != null) {
            // Si se proporciona userId, se añade como query parameter
            // Importante: El nombre del arg aquí "userId" debe coincidir con Routes.Args.USER_ID_ARG
            // y con cómo lo defines en la constante ROUTINE_DETAIL_SCREEN
            route += "?${Args.USER_ID_ARG}=$userId"
        }
        return route
    }

    fun calisthenicsLevelDetail(progressionId: String, levelId: String): String {
        return "$CALISTHENICS_LEVEL_DETAIL_PREFIX/$progressionId/$levelId"
    }

    fun productDetail(productId: String): String {
        return "$PRODUCT_DETAIL_BASE/$productId"
    }

    fun editRoutine(
        userId: String,
        templateId: String? = null,
        customRoutineId: String? = null
    ): String {
        if (userId.isBlank()) {
            // Log.w("Routes", "Se está intentando construir la ruta editRoutine con un userId vacío.")
            // Considera cómo manejar esto. Para la construcción de la ruta, un ID vacío se pasará.
            // La validación real debería ocurrir en el ViewModel.
        }
        // Usa las constantes de Args para construir los query parameters
        val routeBuilder = StringBuilder("$EDIT_ROUTINE_SCREEN_BASE?${Args.USER_ID_ARG}=$userId")
        templateId?.let { routeBuilder.append("&${Args.TEMPLATE_ID_ARG}=$it") }
        customRoutineId?.let { routeBuilder.append("&${Args.CUSTOM_ROUTINE_ID_ARG}=$it") }
        return routeBuilder.toString()
    }
}