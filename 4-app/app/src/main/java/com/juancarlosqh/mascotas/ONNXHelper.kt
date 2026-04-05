package com.juancarlosqh.mascotas

import android.content.Context
import ai.onnxruntime.*
import java.nio.FloatBuffer

object OnnxHelper {

    private var session: OrtSession? = null

    fun init(context: Context) {
        val env = OrtEnvironment.getEnvironment()
        val modelBytes = context.assets.open("best_model_efficientnet_b3.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    fun predict(input: FloatArray): FloatArray {
        val shape = longArrayOf(1, 300, 300, 3)

        val tensor = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            FloatBuffer.wrap(input),
            shape
        )

        val result = session!!.run(
            mapOf(session!!.inputNames.first() to tensor)
        )

        return (result[0].value as Array<FloatArray>)[0]
    }
}