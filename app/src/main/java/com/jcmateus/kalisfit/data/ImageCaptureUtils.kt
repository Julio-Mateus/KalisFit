package com.jcmateus.kalisfit.data


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
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
    onImageReady: (File) -> Unit,
    onError: ((Exception) -> Unit)? = null
) {
    val activity = context as? ComponentActivity
    if (activity == null) {
        Log.e("CaptureImage", "Context is not a ComponentActivity. Provided context: $context")
        onError?.invoke(IllegalStateException("Context is not a ComponentActivity. Provided context: $context"))
        return // Retorna de la función captureComposableAsImage aquí
    }

    val composeView = ComposeView(activity).apply { // Usar 'activity' como contexto aquí
        setContent {
            composable()
        }
    }

    // Acceder a decorView a través de la propiedad window de la Activity
    val decorView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
    if (decorView == null) {
        Log.e("CaptureImage", "DecorView's content view is null.")
        onError?.invoke(IllegalStateException("DecorView's content view is null."))
        return
    }
    decorView.addView(composeView)

    composeView.doOnPreDraw {
        val targetWidth = 1080
        // val maxHeight = 1920 // Opcional

        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        if (composeView.width <= 0 || composeView.height <= 0) {
            Log.e("CaptureImage", "ComposeView has zero dimensions after layout. Width: ${composeView.width}, Height: ${composeView.height}")
            onError?.invoke(IllegalStateException("ComposeView has zero dimensions after layout."))
            // Asegurarse de quitar la vista incluso si hay un error temprano
            (composeView.parent as? ViewGroup)?.removeView(composeView)
            return@doOnPreDraw
        }

        var bitmap: Bitmap? = null
        try {
            Log.d("CaptureImage", "Creating bitmap with Width: ${composeView.width}, Height: ${composeView.height}")
            bitmap = Bitmap.createBitmap(
                composeView.width,
                composeView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)
            Log.d("CaptureImage", "Bitmap drawn successfully.")

            val file = File(context.cacheDir, "kalisfit_summary_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            }
            Log.d("CaptureImage", "Image saved to: ${file.absolutePath}")
            onImageReady(file)

        } catch (e: IllegalArgumentException) { // Específico para createBitmap si las dimensiones son inválidas
            Log.e("CaptureImage", "Error creating bitmap: Invalid dimensions or config.", e)
            onError?.invoke(e)
        } catch (e: OutOfMemoryError) {
            Log.e("CaptureImage", "OutOfMemoryError during bitmap creation or processing.", e)
            onError?.invoke(RuntimeException("OutOfMemoryError during image capture",e)) // Envolver en RuntimeException si onError espera Exception
        }
        catch (e: Exception) {
            Log.e("CaptureImage", "Error during bitmap creation or saving", e)
            onError?.invoke(e)
        } finally {
            // No reciclar el bitmap aquí activamente a menos que estés 100% seguro y tengas fugas.
            // El GC debería manejarlo. Reciclarlo prematuramente puede causar crashes si
            // hay alguna referencia pendiente (ej. en el callback onImageReady antes de que termine de usarse).
            // bitmap?.recycle()

            (composeView.parent as? ViewGroup)?.removeView(composeView)
            Log.d("CaptureImage", "ComposeView removed from parent.")
        }
    }
}