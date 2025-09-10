package com.jcmateus.kalisfit.util

import android.util.Log
import com.google.android.gms.maps.model.LatLng

fun buildStaticMapUrl(
    routePoints: List<LatLng>,
    apiKey: String,
    width: Int = 760, // ~380dp @ 2x densidad
    height: Int = 360, // ~180dp @ 2x densidad
    pathColor: String = "0xC2850BFF", // Tu color primario (MustardDark) sin transparencia para que se vea bien
    pathWeight: Int = 4 // Un grosor decente para la imagen estática
): String {
    if (routePoints.isEmpty() || apiKey.isBlank()) {
        Log.w("buildStaticMapUrl", "Puntos de ruta vacíos o API Key no proporcionada.")
        return "" // Retorna vacío o una URL a una imagen placeholder si la API Key o los puntos faltan
    }

    val baseUrl = "https://maps.googleapis.com/maps/api/staticmap?"
    val sizeParam = "size=${width}x$height"
    val mapTypeParam = "maptype=roadmap" // Puedes probar "terrain" o "satellite" también
    val keyParam = "key=$apiKey"

    // Path (polilínea)
    // La API de Mapas Estáticos tiene un límite en la longitud de la URL.
    // Si la ruta es muy larga (muchos puntos), podrías necesitar simplificarla (algoritmo de Douglas-Peucker)
    // o usar "Encoded Polyline Algorithm". Por ahora, unimos directamente.
    val pathData = routePoints.joinToString(separator = "|") { "${it.latitude},${it.longitude}" }
    val pathParam = "path=color:$pathColor|weight:$pathWeight|$pathData"

    // Marcadores (inicio y fin)
    val markers = mutableListOf<String>()
    routePoints.firstOrNull()?.let {
        // Para marcadores personalizados con URL: markers=icon:URL_ICONO|lat,lng
        // Para marcadores estándar: markers=color:green|label:S|lat,lng (S para Start)
        markers.add("markers=color:0x00A000FF|label:I|${it.latitude},${it.longitude}") // Verde para inicio
    }
    if (routePoints.size > 1) {
        routePoints.lastOrNull()?.let {
            markers.add("markers=color:0xD50000FF|label:F|${it.latitude},${it.longitude}") // Rojo para fin
        }
    }
    val markersParam = markers.joinToString(separator = "&") // Se une con '&' porque son parámetros separados si hay varios 'markers='

    // Construir URL final
    // Es importante que el path y los marcadores no hagan la URL demasiado larga.
    // El parámetro "visible" puede ser una alternativa a los puntos individuales si la URL es muy larga.
    // e.g., visible=lat1,lng1|lat2,lng2|...

    val finalUrl = StringBuilder(baseUrl)
    finalUrl.append(sizeParam)
    finalUrl.append("&$mapTypeParam")
    if (pathParam.isNotBlank() && pathData.isNotEmpty()) finalUrl.append("&$pathParam")
    if (markersParam.isNotBlank()) finalUrl.append("&$markersParam")
    finalUrl.append("&$keyParam")

    Log.d("buildStaticMapUrl", "Generated Static Map URL: $finalUrl")
    return finalUrl.toString()
}