package com.jcmateus.kalisfit.model

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import androidx.compose.ui.unit.dp

/**
 * Shape para una TopAppBar con una onda en su borde inferior.
 * @param period Multiplicador para la longitud de onda. Mayor valor = onda más estirada.
 * @param amplitudeFactor Factor para la altura de la onda (respecto a la altura total de la barra).
 */
class WavyTopAppBarShape(
    private val period: Float = 1.5f, // Controla cuántas "ondas completas" caben. Ajustar al gusto.
    private val amplitudeFactor: Float = 0.15f // 15% de la altura de la barra como amplitud de onda
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val amplitude = size.height * amplitudeFactor // Amplitud de la onda

        path.moveTo(0f, 0f) // Arriba izquierda
        path.lineTo(0f, size.height - amplitude) // Baja hasta el inicio de la onda en el lado izquierdo

        // Dibuja la onda a lo largo del borde inferior
        // Queremos que la onda termine en "valle" en ambos extremos para que se una bien con los lados
        for (x in 0..size.width.toInt()) {
            val angle = (x / (size.width / period)) * (2 * PI) // Mapea x al ángulo para el seno
            // Usamos -cos para que empiece y termine en valle (punto más bajo de la onda)
            // y se ajuste bien al borde recto.
            val y = size.height - amplitude - (amplitude * kotlin.math.cos(angle.toFloat())) / 2  + amplitude / 2
            path.lineTo(x.toFloat(), y.coerceIn(0f, size.height)) // Coerce para no salirse de los límites
        }

        path.lineTo(size.width, size.height - amplitude) // Llega al inicio de la onda en el lado derecho
        path.lineTo(size.width, 0f) // Arriba derecha
        path.close()
        return Outline.Generic(path)
    }
}
/**
 * Shape para una NavigationBar con una onda en su borde superior.
 * @param period Multiplicador para la longitud de onda.
 * @param amplitudeFactor Factor para la altura de la onda (respecto a la altura total de la barra).
 */
class WavyNavigationBarShape(
    private val period: Float = 1.5f,
    private val amplitudeFactor: Float = 0.15f // 15% de la altura de la barra
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val amplitude = size.height * amplitudeFactor

        path.moveTo(0f, size.height) // Abajo izquierda

        // Sube hasta el final de la onda en el lado izquierdo (valle)
        path.lineTo(0f, amplitude)

        // Dibuja la onda a lo largo del borde superior
        for (x in 0..size.width.toInt()) {
            val angle = (x / (size.width / period)) * (2 * PI)
            val y = amplitude + (amplitude * kotlin.math.cos(angle.toFloat())) / 2 - amplitude / 2
            path.lineTo(x.toFloat(), y.coerceIn(0f, size.height))
        }

        // Sube hasta el final de la onda en el lado derecho (valle)
        path.lineTo(size.width, amplitude)
        path.lineTo(size.width, size.height) // Abajo derecha
        path.close()
        return Outline.Generic(path)
    }
}
/**
 * Shape para el borde de un Drawer con una onda.
 * Asume que el drawer se abre desde la izquierda y ondula el borde derecho.
 * @param periodFactor Factor para la "longitud" de cada onda en relación a la altura.
 * @param amplitude Amplitud de la onda en Dp.
 */
enum class WaveSide { START, END }
class WavyDrawerShape(
    private val periodFactor: Float = 1.0f, // Factor para la longitud de onda (más pequeño = más ondas)
    private val amplitude: Float = 20f,   // Amplitud de la onda en Dp
    private val waveSide: WaveSide = WaveSide.END // Por defecto al final (derecha en LTR)
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val waveAmplitudePx = with(density) { amplitude.dp.toPx() }
        val periodPx = size.height * periodFactor // Longitud de una onda completa (ajustar factor)

        val isLtr = layoutDirection == LayoutDirection.Ltr
        val drawWaveOnEndSide = (isLtr && waveSide == WaveSide.END) || (!isLtr && waveSide == WaveSide.START)

        if (drawWaveOnEndSide) {
            // Dibuja el borde izquierdo recto
            path.moveTo(0f, 0f)
            path.lineTo(0f, size.height)
            // Dibuja el borde inferior recto
            path.lineTo(size.width - waveAmplitudePx, size.height) // Asumiendo que la onda "quita" espacio

            // Dibuja la onda en el borde DERECHO (de abajo hacia arriba)
            var y = size.height
            while (y > 0) {
                val angle = ( (size.height - y) / periodPx) * 2 * Math.PI
                val x = size.width - waveAmplitudePx + (waveAmplitudePx * kotlin.math.cos(angle.toFloat())) / 2f - waveAmplitudePx / 2f
                path.lineTo(x.coerceAtLeast(0f).coerceAtMost(size.width), y) // Asegura que x esté dentro de los límites
                y -= 1f // Podrías optimizar esto, pero 1px de step es preciso
            }
            // Dibuja el borde superior (desde la onda hasta la esquina superior izquierda)
            path.lineTo(size.width - waveAmplitudePx, 0f) // Asumiendo que la onda "quita" espacio
            path.lineTo(0f,0f) // Cierra en la esquina superior izquierda

        } else { // Dibuja la onda en el borde IZQUIERDO (START)
            // Dibuja el borde derecho recto
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            // Dibuja el borde inferior recto
            path.lineTo(waveAmplitudePx, size.height) // Asumiendo que la onda "quita" espacio

            // Dibuja la onda en el borde IZQUIERDO (de abajo hacia arriba)
            var y = size.height
            while (y > 0) {
                val angle = ((size.height - y) / periodPx) * 2 * Math.PI
                // La onda ahora se calcula desde 'waveAmplitudePx' hacia la derecha
                val x = waveAmplitudePx - (waveAmplitudePx * kotlin.math.cos(angle.toFloat())) / 2f + waveAmplitudePx / 2f
                path.lineTo(x.coerceAtLeast(0f).coerceAtMost(size.width), y)
                y -= 1f
            }
            // Dibuja el borde superior (desde la onda hasta la esquina superior derecha)
            path.lineTo(waveAmplitudePx, 0f) // Asumiendo que la onda "quita" espacio
            path.lineTo(size.width,0f) // Cierra en la esquina superior derecha
        }

        path.close()
        return Outline.Generic(path)
    }
}
// Nueva Shape para la onda en el borde inferior del encabezado
class WavyBottomHeaderShape(
    private val periodFactor: Float = 0.5f, // Factor para la longitud de onda (respecto al ancho)
    private val amplitude: Float = 10f    // Amplitud de la onda en Dp (altura de la onda)
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val waveAmplitudePx = with(density) { amplitude.dp.toPx() }
        // La longitud de una onda completa puede ser una fracción del ancho total
        val periodWidthPx = size.width * periodFactor

        // Punto de inicio: esquina superior izquierda
        path.moveTo(0f, 0f)
        // Línea hacia esquina superior derecha
        path.lineTo(size.width, 0f)
        // Línea hacia el inicio de la onda en el borde inferior derecho
        path.lineTo(size.width, size.height - waveAmplitudePx) // Comienza la onda un poco más arriba si la amplitud es positiva

        // Dibuja la onda en el borde INFERIOR (de derecha a izquierda)
        var x = size.width
        while (x > 0) {
            // El ángulo depende de la posición x relativa al inicio del ciclo de la onda
            // Queremos que el coseno comience en su pico (o valle) en los bordes para un look simétrico,
            // o ajustarlo para que comience desde cero.
            // Para una onda que "cuelga", empezamos desde size.height y sumamos/restamos la onda.
            // El ángulo se calcula para completar ciclos a lo largo de 'periodWidthPx'
            val angle = ((size.width - x) / periodWidthPx) * 2 * Math.PI
            val y = (size.height - waveAmplitudePx / 2) + (waveAmplitudePx / 2 * kotlin.math.cos(angle.toFloat()))
            path.lineTo(x, y.coerceAtMost(size.height)) // No permitir que la onda exceda el alto original del componente
            x -= 1f // Podrías optimizar el step
        }

        // Línea desde el final de la onda (borde inferior izquierdo) hacia la esquina superior izquierda
        path.lineTo(0f, size.height - waveAmplitudePx) // Conecta con el inicio de la onda en el lado izquierdo
        path.close() // Cierra el path volviendo a (0,0)

        return Outline.Generic(path)
    }
}