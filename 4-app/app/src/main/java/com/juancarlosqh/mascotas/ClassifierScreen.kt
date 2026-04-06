package com.juancarlosqh.mascotas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifierScreen(viewModel: MainViewModel) {

    val context = LocalContext.current
    val result by viewModel.result.collectAsState()
    var showCamera by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf( false) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcherPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcherPermission.launch(Manifest.permission.CAMERA)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {
            val source = ImageDecoder.createSource(context.contentResolver, it)
            val bmp = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }

            bitmap = bmp
            viewModel.classify(bmp)
        }
    }

    if (showCamera) {
        CameraScreen(
            viewModel = viewModel,
            onBack = { showCamera = false }
        )
    } else if (showAbout) {
        AboutScreen(
            onClick = {
                showAbout = false
                viewModel.clearResult()
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Clasificador de mascotas",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold

                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            showAbout = true
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puede cargar una imagen desde su galeria para que la aplicación pueda predecir la raza de su mascota",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = {
                            launcher.launch("image/*")
                        }) {
                            Text(
                                text = "Seleccionar imagen"
                            )
                        }

                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(300.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (result != "Sin resultado") {
                            Text("Predicción: $result")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Si desea realizar una predicción en tiempo real puede utilizar la cámara de su dispositivo",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                showCamera = true
                            } else {
                                launcherPermission.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Text(
                                text = "Abrir cámara"
                            )
                        }
                    }
                }
            }
        }
    }
}