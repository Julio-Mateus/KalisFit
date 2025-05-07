package com.jcmateus.kalisfit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jcmateus.kalisfit.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontFamily = Inter, fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = Inter, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontSize = 12.sp)
)
