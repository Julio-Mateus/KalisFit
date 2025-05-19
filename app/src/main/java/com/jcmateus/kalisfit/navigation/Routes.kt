package com.jcmateus.kalisfit.navigation

object Routes {
    // --- Rutas de Autenticación y Onboarding ---
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_SUCCESS = "onboarding_success"

    // --- Contenedor Principal con BottomNav y TopAppBar ---
    // Esta es la ruta que carga KalisMainScreen, la cual contiene su propio NavHost
    // para las pestañas del BottomNavigationBar.
    const val MAIN_CONTENT = "main_content"

    // --- Rutas gestionadas por el NavHost INTERNO de KalisMainScreen ---
    // Estas constantes pueden seguir existiendo aquí si las usas para construir
    // los BottomNavItem o para referencia, PERO NO serán destinos directos
    // del KalisNavGraph principal. El KalisNavGraph principal solo navega a MAIN_CONTENT.
    const val HOME = "home_screen" // Ejemplo: Renombrado para evitar confusión con una posible ruta de nivel superior
    const val PROFILE_TAB = "profile_tab_screen" // Ejemplo: Renombrado para diferenciarlo de EDIT_PROFILE
    // const val ROUTINES_TAB = "routines_tab_screen" // Si necesitas una constante para la pestaña de rutinas
    // const val HISTORY_TAB = "history_tab_screen"   // Si necesitas una constante para la pestaña de historial

    // --- Rutas de Nivel Superior (Sin BottomNav) ---
    // Estas son accesibles desde pantallas dentro de MAIN_CONTENT (usando el mainNavController)
    // o desde el TopAppBar.
    const val ROUTINE_DETAIL_PREFIX = "routine_detail" // Prefijo para la ruta con argumento
    const val ROUTINE_DETAIL = "$ROUTINE_DETAIL_PREFIX/{rutinaId}" // Ruta completa para ver/ejecutar una rutina
    const val ROUTINE_SUCCESS = "routine_success" // Pantalla después de completar una rutina

    const val EDIT_PROFILE = "edit_profile_screen" // Para la pantalla de edición de perfil (desde TopAppBar o ProfileTab)
    const val SETTINGS = "settings_screen"         // Para la pantalla de configuración (desde TopAppBar)

    // const val TIPS_EXPLORER = "tips_explorer_screen" // Si decides tener una pantalla para explorar todos los tips
    const val TIPS = "tips" // Mantengo esta si aún tienes un uso específico para ella
    // que no sea una pantalla de exploración completa.
    // Si 'TIPS' es para explorar, considera renombrarla a TIPS_EXPLORER.
}