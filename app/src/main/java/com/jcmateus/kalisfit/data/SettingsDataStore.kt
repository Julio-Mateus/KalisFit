package com.jcmateus.kalisfit.data


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Define el DataStore como una extensión de Context.
// "kalis_settings" será el nombre del archivo físico donde DataStore guardará las preferencias.
// Solo debe haber UNA instancia de DataStore por este nombre ("kalis_settings") en tu aplicación.
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kalis_settings")