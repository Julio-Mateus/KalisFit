package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme

@Composable
fun ResumenVisualCard(resumen: ResumenSemanal) {
    // Para la captura de imagen, a veces MaterialTheme no se propaga automáticamente
    // si capturas fuera de la jerarquía principal de la UI.
    // Envolver con KalisFitTheme asegura que use el tema correcto.
    KalisFitTheme(darkTheme = false) { // O el tema que desees para la tarjeta visual

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(320.dp)
                .height(480.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Usa surface para el fondo de la card
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer, // Un tono más claro de tu primario
                                MaterialTheme.colorScheme.primary         // Tu primario
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- SECCIÓN TÍTULO ---
                    Text(
                        text = "💪 Progreso Semanal 💪",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer, // Color de texto sobre el degradado
                        textAlign = TextAlign.Center
                    )

                    // --- SECCIÓN DETALLES DEL RESUMEN ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "🏋️ Rutinas: ${resumen.rutinas}",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer // o .onSurface si el fondo del Box fuera surface
                        )
                        Text(
                            "⏱ Tiempo Total: ${formatSecondsToMinutesSeconds(resumen.tiempoTotal)}",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "🤸 Ejercicios Totales: ${resumen.totalEjercicios}",
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (resumen.ejerciciosPorTiempo > 0) {
                            Text(
                                "⏱️ Ejercicios por Tiempo: ${resumen.ejerciciosPorTiempo}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) // Ligeramente menos énfasis
                            )
                        }
                        if (resumen.ejerciciosPorRepeticiones > 0) {
                            Text(
                                "🔄 Ejercicios por Reps: ${resumen.ejerciciosPorRepeticiones}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (resumen.objetivosRecurrentes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "🎯 Objetivos Destacados:",
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            resumen.objetivosRecurrentes.forEach { objetivo ->
                                Text(
                                    "• $objetivo",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // --- SECCIÓN LOGO Y ESLOGAN ---
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "Logo KalisFit",
                            modifier = Modifier.size(72.dp)
                            // Considera tinting el logo si es un icono simple:
                            // colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "KalisFit",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Transforma tu cuerpo. Cambia tu vida.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), // Menos prominente
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

