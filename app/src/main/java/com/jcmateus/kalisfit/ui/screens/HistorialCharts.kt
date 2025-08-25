package com.jcmateus.kalisfit.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.intl.Locale
import com.jcmateus.kalisfit.model.ProgresoRutina
import me.bytebeats.views.charts.bar.BarChartData
import me.bytebeats.views.charts.bar.render.bar.SimpleBarDrawer
import me.bytebeats.views.charts.bar.render.xaxis.SimpleXAxisDrawer
import me.bytebeats.views.charts.bar.render.yaxis.SimpleYAxisDrawer
import me.bytebeats.views.charts.simpleChartAnimation
import me.bytebeats.views.charts.bar.BarChart
import me.bytebeats.views.charts.bar.render.label.SimpleLabelDrawer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun RutinasBarChart(historial: List<ProgresoRutina>, modifier: Modifier = Modifier) {
    // Formateador para convertir Timestamp a "yyyy-MM-dd"
    // Es buena idea definirlo fuera del map para eficiencia si la lista es grande
    // Puedes ajustar el TimeZone si es necesario, por defecto usará el del dispositivo.
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Opcional: dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val barras = historial.groupBy {
        // Convierte el Timestamp a Date, luego formatea a String
        val date = it.fecha.toDate()
        dateFormat.format(date) // Esto te dará algo como "2023-10-27"
    }
        .map { (fechaString, items) -> // fechaString es ahora el resultado del formato
            BarChartData.Bar(
                label = fechaString, // Usar la fecha formateada
                value = items.size.toFloat(),
                color = Color(0xFF3F51B5)
            )
        }
    val barChartData = BarChartData(barras)
    BarChart(
        barChartData = barChartData,
        modifier = modifier,
        animation = simpleChartAnimation(),
        barDrawer = SimpleBarDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer(),
        labelDrawer = SimpleLabelDrawer()
    )
}
@Composable
fun TiempoBarChart(historial: List<ProgresoRutina>, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Opcional: dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val barras = historial.groupBy {
        val date = it.fecha.toDate()
        dateFormat.format(date)
    }
        .map { (fechaString, items) ->
            val totalTiempo = items.sumOf { it.tiempoTotalSesionSegundos }
            BarChartData.Bar(
                label = fechaString,
                value = totalTiempo.toFloat(),
                color = Color(0xFF4CAF50)
            )
        }

    val barChartData = BarChartData(barras)

    BarChart(
        barChartData = barChartData,
        modifier = modifier,
        animation = simpleChartAnimation(),
        barDrawer = SimpleBarDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer(),
        labelDrawer = SimpleLabelDrawer()
    )
}

