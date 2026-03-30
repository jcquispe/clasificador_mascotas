package com.juancarlosqh.mascotas

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class ONNXClassifier(context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val classNames: List<String>

    init {
        val modelPath = copyModelToFile(context, "best_model_efficientnet_b3.onnx")
        session = env.createSession(modelPath)
        classNames = loadClassNames(context, "classes.json")
    }

    fun predict(bitmap: Bitmap, topK: Int = 5): List<Pair<String, Float>> {
        val inputTensor = preprocess(bitmap)

        val inputs = mapOf("input" to inputTensor)
        val results = session.run(inputs)
        val output = results[0].value as Array<FloatArray>
        val logits = output[0]

        val exp = logits.map { Math.exp(it.toDouble()) }
        val sumExp = exp.sum()
        val probs = exp.map { (it / sumExp).toFloat() }
        val topIndices = probs.indices.sortedByDescending { probs[it] }.take(topK)

        return topIndices.map { i -> classNames[i] to probs[i] }
    }

    private fun preprocess(bitmap: Bitmap): OnnxTensor {
        val width = 300
        val height = 300
        val floatValues = FloatArray(1 * height * width * 3)
        var idx = 0

        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF).toFloat() / 127.5f) - 1.0f
                val g = ((pixel shr 8 and 0xFF).toFloat() / 127.5f) - 1.0f
                val b = ((pixel and 0xFF).toFloat() / 127.5f) - 1.0f
                floatValues[idx] = r
                floatValues[idx + 1] = g
                floatValues[idx + 2] = b
                idx += 3
            }
        }
        val shape = longArrayOf(1, height.toLong(), width.toLong(), 3)
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(floatValues), shape)
    }

    private fun copyModelToFile(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    private fun loadClassNames(context: Context, assetName: String): List<String> {
        val jsonStr = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val jsonArray = JSONObject("{\"classes\":$jsonStr}").getJSONArray("classes")
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }
}