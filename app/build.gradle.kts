import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    alias(libs.plugins.google.services) // Firebase
    id("kotlin-parcelize")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()){
    localPropertiesFile.inputStream().use {localProperties.load(it)}
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")


android {
    namespace = "com.jcmateus.kalisfit"
    compileSdk = 35

    defaultConfig {
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        applicationId = "com.jcmateus.kalisfit"
        minSdk = 26 // ACTUALIZADO: Requerido por Health Connect
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Por defecto, debuggable es true para el build type 'debug',
            // lo que hace que BuildConfig.DEBUG también sea true.
            // Puedes añadir explícitamente si quieres ser muy claro:
            isDebuggable = true
            buildConfigField("boolean", "DEBUG", "true")
            versionNameSuffix = "-debug"   // Opcional: añade un sufijo al nombre de versión para debug
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeBom.get()
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Compose Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug:17.1.1")

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    /*
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
     */

    // Coil
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup:javapoet:1.13.0")
    constraints {
        implementation("com.squareup:javapoet:1.13.0") {
            because("Force this version to avoid runtime errors")
        }
    }

    implementation(libs.lottie.compose)
    implementation(libs.play.services.auth)
    implementation(libs.material.icons.extended)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.compose.charts)

    // Jetpack DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    implementation("io.coil-kt:coil-gif:2.4.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.accompanist:accompanist-placeholder-material:0.36.0") // Revisa la última versión

    // Health Connect para métricas de Salud (Ritmo cardíaco, etc.)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha11")

    // Play Services Wearable para comunicación móvil <-> reloj
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    implementation("androidx.compose.material:material:1.7.0")
}