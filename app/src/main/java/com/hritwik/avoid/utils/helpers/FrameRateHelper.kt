package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import java.util.Locale

object FrameRateHelper {

    fun findSurfaceView(root: View?): SurfaceView? {
        if (root == null) return null
        if (root is SurfaceView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val surface = findSurfaceView(child)
                if (surface != null) return surface
            }
        }
        return null
    }

    fun applyFrameRate(surfaceView: SurfaceView?, frameRate: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        if (frameRate <= 0f) return false
        val surface = surfaceView?.holder?.surface ?: return false
        if (!surface.isValid) return false
        applyFrameRate(surface, frameRate)
        return true
    }

    suspend fun requestFrameRate(
        surfaceProvider: () -> SurfaceView?,
        frameRate: Float,
        attempts: Int = 3,
        delayMs: Long = 200
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        if (frameRate <= 0f) return false
        repeat(attempts.coerceAtLeast(1)) {
            if (applyFrameRate(surfaceProvider(), frameRate)) {
                return true
            }
            delay(delayMs)
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun applyFrameRate(surface: Surface, frameRate: Float) {
        surface.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
    }

    fun showFrameRateToast(context: Context, frameRate: Float) {
        val density = context.resources.displayMetrics.density
        val yOffset = (48 * density).toInt()
        val rateLabel = String.format(Locale.US, "%s hz", frameRate.toString())
        Toast.makeText(context, "Screen refresh rate changed to $rateLabel", Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffset)
            show()
        }
    }
}
