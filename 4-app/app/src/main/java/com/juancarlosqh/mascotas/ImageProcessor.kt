package com.juancarlosqh.mascotas

import android.graphics.Bitmap

object ImageProcessor {
    fun preprocess(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        val result = FloatArray(1 * 300 * 300 * 3)

        var index = 0

        for (y in 0 until 300) {
            for (x in 0 until 300) {
                val pixel = resized.getPixel(x, y)

                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                result[index++] = (r / 255f - 0.485f) / 0.229f
                result[index++] = (g / 255f - 0.456f) / 0.224f
                result[index++] = (b / 255f - 0.406f) / 0.225f
            }
        }

        return result
    }
}