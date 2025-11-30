package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.graphics.Bitmap

@Suppress("DEPRECATION")
object BlurUtils {

    private const val MAX_RADIUS = 25f

    fun blurBitmap(context: Context, source: Bitmap, radius: Float): Bitmap {
        val clampedRadius = radius.coerceIn(0f, MAX_RADIUS)
        if (clampedRadius <= 0f || source.width == 0 || source.height == 0) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val inputBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val outputBitmap = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, Bitmap.Config.ARGB_8888)

        val renderScript = android.renderscript.RenderScript.create(context)
        val inputAllocation = android.renderscript.Allocation.createFromBitmap(
            renderScript,
            inputBitmap,
        )
        val outputAllocation = android.renderscript.Allocation.createFromBitmap(
            renderScript,
            outputBitmap,
        )
        val script = android.renderscript.ScriptIntrinsicBlur.create(
            renderScript,
            android.renderscript.Element.U8_4(renderScript),
        )
        script.setRadius(clampedRadius)
        script.setInput(inputAllocation)
        script.forEach(outputAllocation)
        outputAllocation.copyTo(outputBitmap)

        inputAllocation.destroy()
        outputAllocation.destroy()
        script.destroy()
        renderScript.destroy()

        return outputBitmap
    }
}
