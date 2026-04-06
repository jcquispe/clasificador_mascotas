package com.juancarlosqh.mascotas

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _result = MutableStateFlow("Sin resultado")
    val result: StateFlow<String> = _result
    private var lastExecutionTime = 0L

    fun classify(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastExecutionTime < 500) return

        lastExecutionTime = currentTime

        viewModelScope.launch(Dispatchers.Default) {
            val input = ImageProcessor.preprocess(bitmap)
            val output = OnnxHelper.predict(input)

            val index = output.indices.maxByOrNull { output[it] } ?: -1
            val classes = ClassesLoader.get()

            _result.value = if (index in classes.indices) {
                classes[index]
            } else {
                "Clase desconocida"
            }
        }
    }

    fun clearResult() {
        _result.value = "Sin resultado"
    }
}