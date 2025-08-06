package com.jcmateus.kalisfit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.BuildConfig
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class KalisFitApplication : Application() {


    companion object {

        // IDs para tus canales de notificación

        const val TRAINING_REMINDER_CHANNEL_ID = "training_reminder_channel"

        const val STOIC_TIP_CHANNEL_ID = "stoic_tip_channel"

        const val GENERAL_REMINDERS_CHANNEL_ID = "kalisfit_general_reminders_channel"

        // Puedes añadir más canales si los necesitas (ej. para noticias, etc.)

    }


    override fun onCreate() {

        super.onCreate()
        Log.d("AppCheckDebug", "KalisFitApplication onCreate - BuildConfig.DEBUG: ${BuildConfig.DEBUG}") // Log para verificar BuildConfig
        if (BuildConfig.DEBUG) {
            Log.d("AppCheckDebug", "Attempting to install DebugAppCheckProviderFactory")
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("AppCheckDebug", "DebugAppCheckProviderFactory installed (or attempted)")
        } else {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        /*
        // *** INICIO: CÓDIGO DE FIREBASE APP CHECK ***
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(

            PlayIntegrityAppCheckProviderFactory.getInstance(), // <--- ¡Aquí está el cambio!

        )

        // *** FIN: CÓDIGO DE FIREBASE APP CHECK ***
         */


        createNotificationChannels()

    }


    private fun createNotificationChannels() {

        // La creación de canales solo es necesaria en Android Oreo (API 26) y superior

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Canal para Recordatorios de Entrenamiento

            val trainingChannelName = "Recordatorios de Entrenamiento" // Nombre visible para el usuario

            val trainingChannelDescription = "Canal para recordatorios de sesiones de entrenamiento."

            val trainingChannelImportance = NotificationManager.IMPORTANCE_HIGH // Importancia alta para recordatorios

            val trainingChannel = NotificationChannel(

                TRAINING_REMINDER_CHANNEL_ID,

                trainingChannelName,

                trainingChannelImportance

            ).apply {

                description = trainingChannelDescription

                // Aquí puedes configurar más opciones del canal si lo deseas:

                enableLights(true)

                lightColor = Color.RED

                enableVibration(true)

                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

                setShowBadge(true) // Mostrar punto en el icono de la app

            }


            // Canal para Consejos Estoicos

            val stoicTipChannelName = "Consejos Estoicos"

            val stoicTipChannelDescription = "Canal para recibir consejos y filosofía estoica."

            val stoicTipChannelImportance = NotificationManager.IMPORTANCE_DEFAULT // Importancia por defecto para tips

            val stoicTipChannel = NotificationChannel(

                STOIC_TIP_CHANNEL_ID,

                stoicTipChannelName,

                stoicTipChannelImportance

            ).apply {

                description = stoicTipChannelDescription

            }


            // Obtener el servicio NotificationManager y registrar los canales

            val notificationManager: NotificationManager =

                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


            notificationManager.createNotificationChannel(trainingChannel)

            notificationManager.createNotificationChannel(stoicTipChannel)


            // Asegúrate de que R.string.notification_channel_general_reminders_name y _description existan

            // en tus archivos strings.xml

            val generalChannelName = getString(R.string.notification_channel_general_reminders_name) // Ejemplo: "Recordatorios Generales"

            val generalChannelDescription = getString(R.string.notification_channel_general_reminders_description) // Ejemplo: "Notificaciones y recordatorios generales"

            val generalChannelImportance = NotificationManager.IMPORTANCE_DEFAULT // O IMPORTANCE_HIGH según necesites

            val generalChannel = NotificationChannel(

                GENERAL_REMINDERS_CHANNEL_ID, // Usando la constante correcta

                generalChannelName,

                generalChannelImportance

            ).apply {

                description = generalChannelDescription

                // Configura luces, vibración, etc., si es necesario para este canal

                enableLights(true)

                lightColor = Color.BLUE

                enableVibration(true)

            }

            notificationManager.createNotificationChannel(generalChannel)

        }

    }


    object FirestoreInstance {

        val instance: FirebaseFirestore by lazy {

            Firebase.firestore

        }

    }

}
