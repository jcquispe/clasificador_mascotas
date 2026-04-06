package com.juancarlosqh.mascotas

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen( onClick: () -> Unit ) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de") },
                navigationIcon = {
                    IconButton(onClick = onClick) {
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
                    Image(
                        painter = painterResource(id = R.drawable.logoupea),
                        contentDescription = "Logo UPEA"
                    )

                    Text(
                        text = "PROYECTO FINAL",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Módulo 11 - Machine Learning y Aprendizaje Automático",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maestría en Inteligencia Artificial, Machine Learning y Data Science",
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Desarrollado por: Juan Carlos Quispe Huallpa",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "GitHub",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jcquispe/clasificador_mascotas"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}