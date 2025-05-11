package com.jcmateus.kalisfit.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jcmateus.kalisfit.model.ProgresoRutina
import me.bytebeats.views.charts.bar.BarChartData
import me.bytebeats.views.charts.bar.render.bar.SimpleBarDrawer
import me.bytebeats.views.charts.bar.render.xaxis.SimpleXAxisDrawer
import me.bytebeats.views.charts.bar.render.yaxis.SimpleYAxisDrawer
import me.bytebeats.views.charts.simpleChartAnimation
import me.bytebeats.views.charts.bar.BarChart
import me.bytebeats.views.charts.bar.render.label.SimpleLabelDrawer
@Composable
fun RutinasBarChart(historial: List<ProgresoRutina>, modifier: Modifier = Modifier) {
    val barras = historial.groupBy { it.fecha.take(10) }
        .map { (fecha, items) ->
            BarChartData.Bar(
                label = fecha,
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
    val barras = historial.groupBy { it.fecha.take(10) }
        .map { (fecha, items) ->
            val totalTiempo = items.sumOf { it.tiempoTotal }
            BarChartData.Bar(
                label = fecha,
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

