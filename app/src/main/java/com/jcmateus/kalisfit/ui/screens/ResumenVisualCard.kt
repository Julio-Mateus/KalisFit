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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.ResumenSemanal

@Composable
fun ResumenVisualCard(resumen: ResumenSemanal) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .width(320.dp)
            .height(480.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "💪 Progreso semanal",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = Color.Black
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🏋️ Rutinas: ${resumen.rutinas}", fontSize = 18.sp, color = Color.Black)
                    Text("⏱ Tiempo: ${resumen.tiempoTotal} seg", fontSize = 18.sp, color = Color.Black)
                    if (resumen.objetivosRecurrentes.isNotEmpty()) {
                        Text("🎯 Objetivos más comunes:", fontSize = 16.sp, color = Color.Black)
                        resumen.objetivosRecurrentes.forEach {
                            Text("• $it", fontSize = 14.sp, color = Color.Black)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("KalisFit", fontSize = 18.sp, color = Color.Black)
                    Text(
                        "Transforma tu cuerpo. Cambia tu vida.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

