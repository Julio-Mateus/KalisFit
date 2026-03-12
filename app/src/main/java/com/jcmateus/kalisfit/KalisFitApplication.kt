package com.jcmateus.kalisfit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class KalisFitApplication : Application() {
    companion object {
        const val TRAINING_REMINDER_CHANNEL_ID = "training_reminder_channel"
        const val STOIC_TIP_CHANNEL_ID = "stoic_tip_channel"
        const val GENERAL_REMINDERS_CHANNEL_ID = "kalisfit_general_reminders_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("KalisFitApp", "--- INICIANDO APLICACIÓN ---")

        FirebaseApp.initializeApp(this)

        // ACTIVAR APP CHECK (Necesario porque tu consola lo exige)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // Usamos el Debug Provider para que te permita loguearte desde Android Studio
        if (BuildConfig.DEBUG) {
            Log.d("KalisFitApp", "Modo DEBUG: Instalando DebugAppCheckProviderFactory")
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val trainingChannel = NotificationChannel(
                TRAINING_REMINDER_CHANNEL_ID,
                "Recordatorios de Entrenamiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para recordatorios de sesiones."
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            val stoicTipChannel = NotificationChannel(
                STOIC_TIP_CHANNEL_ID,
                "Consejos Estoicos",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val generalChannel = NotificationChannel(
                GENERAL_REMINDERS_CHANNEL_ID,
                "Recordatorios Generales",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(trainingChannel)
            notificationManager.createNotificationChannel(stoicTipChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    object FirestoreInstance {
        val instance: FirebaseFirestore by lazy { Firebase.firestore }
    }
}