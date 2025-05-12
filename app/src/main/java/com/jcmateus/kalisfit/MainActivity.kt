package com.jcmateus.kalisfit

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.jcmateus.kalisfit.navigation.KalisNavGraph
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KalisFitTheme {
                Surface {
                    val navController = rememberNavController()
                    KalisNavGraph(navController = navController)
                }
            }
        }
    }
}
