package com.jcmateus.kalisfit.navigation


object Routes {
    // --- Rutas de Autenticación y Onboarding ---
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_SUCCESS = "onboarding_success"

    // --- Contenedor Principal (ahora con Drawer, TopAppBar y BottomNav) ---
    // Esta es la ruta que carga KalisMainScreen, la cual contiene su propio NavHost
    // para las pestañas del BottomNavigationBar.
    const val MAIN_CONTENT = "main_content"

    // --- Rutas gestionadas por el NavHost INTERNO de KalisMainScreen (Pestañas del BottomNav) ---
    // Estas constantes definen las rutas para las *pestañas* del BottomNavigationBar.
    // NO son destinos directos del KalisNavGraph principal, sino del NavHost dentro de KalisMainScreen.
    // Los nombres de estas constantes DEBEN coincidir con los `route` definidos en `BottomNavItem.kt`.
    const val HOME_TAB = "home"                 // Coincide con BottomNavItem.Home.route
    const val ROUTINES_TAB = "routines"         // Coincide con BottomNavItem.Routines.route
    const val RUNNING_TAB = "running_tab"       // Coincide con BottomNavItem.Running.route. NUEVO
    const val HISTORY_TAB = "historial"         // Coincide con BottomNavItem.History.route

    // --- Rutas de Nivel Superior (Sin BottomNav, accesibles desde Drawer, TopAppBar o dentro de otras pantallas) ---
    // Estas son navegables usando el `mainNavController`.

    const val PROFILE = "profile_screen"        // NUEVO: Para la pantalla de perfil completa (desde Drawer)
    // Se elimina PROFILE_TAB si "Perfil" ya no es una pestaña.

    const val EDIT_PROFILE = "edit_profile_screen" // Para la pantalla de edición de perfil (desde Drawer o Perfil)
    const val SETTINGS = "settings_screen"         // Para la pantalla de configuración (desde Drawer)

    const val ROUTINE_DETAIL_PREFIX = "routine_detail"
    const val ROUTINE_DETAIL = "$ROUTINE_DETAIL_PREFIX/{rutinaId}"
    const val ROUTINE_SUCCESS = "routine_success"

    const val TIPS = "tips" // Evalúa si todavía necesitas esta ruta específica y cómo se accede
    // const val TIPS_EXPLORER = "tips_explorer_screen"

    // --- Rutas Potenciales Futuras (accesibles desde Drawer o internamente) ---
    // const val RUNNING_DASHBOARD = "running_dashboard_screen" // Si quieres una pantalla de Running más detallada desde el Drawer
     const val STOICISM_CONTENT = "stoicism_content_screen"   // Para la sección de Estoicismo
    // const val GYM_ROUTINES = "gym_routines_explorer"       // Para rutinas filtradas de gimnasio
    // const val CALISTHENICS_ROUTINES = "calisthenics_routines_explorer" // Para rutinas filtradas de calistenia

}