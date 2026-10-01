package com.dbhr.bxsky

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dbhr.bxsky.presentation.camera.LprCameraScreen
import com.dbhr.bxsky.presentation.camera.LprCameraUiState
import com.dbhr.bxsky.vision.camera.CameraManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreenContent()
                }
            }
        }
    }
}

@Composable
fun MainScreenContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var uiState by remember {
            mutableStateOf(
                LprCameraUiState(
                    detection = null,
                    isScanning = true,
                    isTorchEnabled = false
                )
            )
        }

        LprCameraScreen(
            uiState = uiState,
            onTorchToggle = {
                val newTorchState = !uiState.isTorchEnabled
                uiState = uiState.copy(isTorchEnabled = newTorchState)
                cameraManager.setTorchEnabled(newTorchState)
            },
            onPlateConfirm = { plate ->
                Toast.makeText(context, "Placa confirmada: $plate", Toast.LENGTH_SHORT).show()
                uiState = uiState.copy(detection = null)
            },
            onResetScan = {
                uiState = uiState.copy(detection = null)
            },
            onPreviewViewCreated = { previewView ->
                cameraManager.startCamera(previewView) { detected ->
                    uiState = uiState.copy(detection = detected)
                }
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Se requiere acceso a la cámara para escanear placas vehiculares.",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Conceder permiso")
                }
            }
        }
    }
}