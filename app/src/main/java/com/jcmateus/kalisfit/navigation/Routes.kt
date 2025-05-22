package com.jcmateus.kalisfit.navigation


object Routes {
    // --- Rutas de Autenticación y Onboarding ---
    const val SPLASH = "splash_screen" // Hago los nombres un poco más descriptivos añadiendo "_screen" o "_tab"
    const val LOGIN = "login_screen"
    const val REGISTER = "register_screen"
    const val FORGOT_PASSWORD = "forgot_password_screen"
    const val ONBOARDING = "onboarding_screen"
    const val ONBOARDING_SUCCESS = "onboarding_success_screen"

    // --- Contenedor Principal (con Drawer, TopAppBar y BottomNav) ---
    // Ruta que carga KalisMainScreen, que gestiona el NavHost interno para las pestañas.
    const val MAIN_CONTENT = "main_content_screen"

    // --- Pestañas del BottomNavigationBar (gestionadas por el NavHost INTERNO de KalisMainScreen) ---
    // Deben coincidir con los 'route' definidos en BottomNavItem.kt
    const val HOME_TAB = "home_tab"
    const val CALISTHENICS_TAB = "calisthenics_tab"   // NUEVA PESTAÑA
    const val STOICISM_TAB = "stoicism_tab"         // NUEVA PESTAÑA
    const val RUNNING_TAB = "running_tab"           // Se mantiene

    // --- Rutas de Nivel Superior (Sin BottomNav, accesibles desde Drawer, TopAppBar o navegación interna) ---
    // Navegables usando el 'mainNavController' desde KalisNavGraph.

    const val PROFILE_SCREEN = "profile_screen"             // Pantalla de perfil (desde Drawer)
    const val EDIT_PROFILE_SCREEN = "edit_profile_screen"   // Edición de perfil (desde Drawer o Perfil)
    const val SETTINGS_SCREEN = "settings_screen"           // Configuración (desde Drawer)

    // Rutinas y Historial ahora son pantallas de nivel superior accedidas desde el Drawer
    const val ROUTINES_EXPLORER_SCREEN = "routines_explorer_screen" // Antes ROUTINES_TAB
    const val ACTIVITY_HISTORY_SCREEN = "activity_history_screen"   // Antes HISTORY_TAB

    // Detalles y otras pantallas relacionadas con rutinas
    const val ROUTINE_DETAIL_PREFIX = "routine_detail"
    const val ROUTINE_DETAIL_SCREEN = "$ROUTINE_DETAIL_PREFIX/{routineId}" // Añadido "_screen"
    const val ROUTINE_SUCCESS_SCREEN = "routine_success_screen"

    // Rutas para contenido específico o secciones (evalúa si la de Tips sigue siendo necesaria)
    const val TIPS_SCREEN = "tips_screen"
    // const val STOICISM_CONTENT_SCREEN = "stoicism_content_screen" // Esta ruta se convierte en STOICISM_TAB si la pantalla es la misma.
    // Si quisieras una pantalla de "Acerca de Estoicismo" separada en el drawer
    // podrías mantenerla con un nombre diferente. Por ahora, asumimos que
    // STOICISM_TAB es suficiente.
    // =============================================================
    //          NUEVA RUTA PARA DETALLE DE NIVEL DE CALISTENIA
    // =============================================================
    const val CALISTHENICS_LEVEL_DETAIL_PREFIX = "calisthenics_level_detail"
    const val CALISTHENICS_LEVEL_DETAIL_SCREEN = "$CALISTHENICS_LEVEL_DETAIL_PREFIX/{progressionId}/{levelId}"

    // Función helper para construir la ruta de detalle del nivel de calistenia
    fun calisthenicsLevelDetail(progressionId: String, levelId: String): String {
        return "$CALISTHENICS_LEVEL_DETAIL_PREFIX/$progressionId/$levelId"
    }
    // =============================================================

    // Función helper para la ruta de detalle de rutina (ya la tenías implícita, la hago explícita)
    fun routineDetail(routineId: String): String {
        return "$ROUTINE_DETAIL_PREFIX/$routineId"
    }



    // --- Rutas Potenciales Futuras (o para funcionalidades específicas dentro de pantallas) ---
    // const val RUNNING_DASHBOARD_SCREEN = "running_dashboard_screen" // Pantalla detallada de Running (Drawer)
    // const val GYM_ROUTINES_FILTER_SCREEN = "gym_routines_filter_screen"       // Para rutinas filtradas de gimnasio
    // const val CALISTHENICS_ROUTINES_FILTER_SCREEN = "calisthenics_routines_filter_screen" // Para rutinas filtradas de calistenia (si es diferente de la guía de progresión)
}