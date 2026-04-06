package com.juancarlosqh.mascotas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: MainViewModel, onBack: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val result by viewModel.result.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clasificador en tiempo real") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = result,
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            )
        }
    }
    LaunchedEffect(Unit) {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(
                Executors.newSingleThreadExecutor()
            ) { imageProxy ->

                val bitmap = imageProxy.toBitmap(context)

                bitmap?.let {
                    viewModel.classify(it)
                }

                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }
}

fun ImageProxy.toBitmap(context: Context): Bitmap? {

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, width, height),
        100,
        out
    )

    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}