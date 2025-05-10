package com.jcmateus.kalisfit.data


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnPreDraw
import java.io.File
import java.io.FileOutputStream

fun captureComposableAsImage(
    context: Context,
    composable: @Composable () -> Unit,
    onImageReady: (File) -> Unit
) {
    val activity = context as? ComponentActivity ?: return

    val composeView = ComposeView(context).apply {
        setContent {
            MaterialTheme {
                Surface {
                    composable()
                }
            }
        }
    }

    activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        .addView(composeView)

    composeView.doOnPreDraw {
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            composeView.width,
            composeView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)

        val file = File(context.cacheDir, "resumen_kalisfit.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        onImageReady(file)

        (composeView.parent as? ViewGroup)?.removeView(composeView)
    }
}
