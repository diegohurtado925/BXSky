package com.dbhr.bxsky.presentation.camera

import android.content.res.Configuration
import android.graphics.Rect as AndroidRect
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dbhr.bxsky.data.model.PlateDetection

data class LprCameraUiState(
    val detection: PlateDetection? = null,
    val isTorchEnabled: Boolean = false,
    val isScanning: Boolean = true
)

@Composable
fun LprCameraScreen(
    uiState: LprCameraUiState,
    onTorchToggle: () -> Unit = {},
    onPlateConfirm: (String) -> Unit = {},
    onResetScan: () -> Unit = {},
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewViewCreated(this)
                }
            }
        )


        LprViewfinderOverlay(
            modifier = Modifier.fillMaxSize(),
            hasValidPlate = uiState.detection != null,
            isLandscape = isLandscape
        )


        TrackingOverlay(
            detection = uiState.detection,
            modifier = Modifier.fillMaxSize()
        )

        LprTopControls(
            isTorchEnabled = uiState.isTorchEnabled,
            isScanning = uiState.isScanning,
            onTorchToggle = onTorchToggle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isLandscape) 32.dp else 20.dp,
                    vertical = if (isLandscape) 16.dp else 40.dp
                )
                .align(Alignment.TopCenter)
        )


        AnimatedVisibility(
            visible = uiState.detection != null,
            enter = if (isLandscape) slideInHorizontally(initialOffsetX = { it }) + fadeIn()
            else slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = if (isLandscape) slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            else slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
                .padding(if (isLandscape) 24.dp else 20.dp)
        ) {
            uiState.detection?.let { det ->
                PlateResultCard(
                    plateText = det.plateNumber,
                    isLandscape = isLandscape,
                    onConfirm = { onPlateConfirm(det.plateNumber) },
                    onRetry = onResetScan
                )
            }
        }
    }
}

@Composable
fun LprViewfinderOverlay(
    modifier: Modifier = Modifier,
    hasValidPlate: Boolean = false,
    isLandscape: Boolean = false
) {
    val borderColor = if (hasValidPlate) Color(0xFF00E676) else Color(0xFFFFC107)

    Canvas(modifier = modifier) {
        val overlayWidth = size.width
        val overlayHeight = size.height


        val boxWidth = if (isLandscape) overlayWidth * 0.48f else overlayWidth * 0.82f
        val boxHeight = if (isLandscape) boxWidth * 0.45f else boxWidth * 0.45f

        val left = if (isLandscape) (overlayWidth * 0.42f - boxWidth / 2f) else (overlayWidth - boxWidth) / 2f
        val top = (overlayHeight - boxHeight) / 2f

        val roundedRectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(left, top), Size(boxWidth, boxHeight)),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            )
        }

        clipPath(path = roundedRectPath, clipOp = ClipOp.Difference) {
            drawRect(color = Color(0x99000000))
        }

        drawRoundRect(
            color = borderColor.copy(alpha = 0.6f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        val cornerLength = 28.dp.toPx()
        val cornerStroke = 4.dp.toPx()

        drawLine(borderColor, Offset(left, top + cornerLength), Offset(left, top), cornerStroke)
        drawLine(borderColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)

        drawLine(borderColor, Offset(left + boxWidth - cornerLength, top), Offset(left + boxWidth, top), cornerStroke)
        drawLine(borderColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), cornerStroke)

        drawLine(borderColor, Offset(left, top + boxHeight - cornerLength), Offset(left, top + boxHeight), cornerStroke)
        drawLine(borderColor, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), cornerStroke)

        drawLine(borderColor, Offset(left + boxWidth - cornerLength, top + boxHeight), Offset(left + boxWidth, top + boxHeight), cornerStroke)
        drawLine(borderColor, Offset(left + boxWidth, top + boxHeight - cornerLength), Offset(left + boxWidth, top + boxHeight), cornerStroke)
    }
}

@Composable
fun TrackingOverlay(
    detection: PlateDetection?,
    modifier: Modifier = Modifier
) {
    if (detection == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height

        val scaleX = viewWidth / detection.imageWidth.toFloat()
        val scaleY = viewHeight / detection.imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)

        val offsetX = (viewWidth - (detection.imageWidth * scale)) / 2f
        val offsetY = (viewHeight - (detection.imageHeight * scale)) / 2f

        val box = detection.boundingBox

        val left = box.left * scale + offsetX
        val top = box.top * scale + offsetY
        val right = box.right * scale + offsetX
        val bottom = box.bottom * scale + offsetY

        val rectWidth = right - left
        val rectHeight = bottom - top

        drawRoundRect(
            color = Color(0xFF00E676),
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 3.5.dp.toPx())
        )

        drawRoundRect(
            color = Color(0x3300E676),
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
    }
}

@Composable
fun LprTopControls(
    isTorchEnabled: Boolean,
    isScanning: Boolean,
    onTorchToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xAA1E1E1E),
            modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isScanning) Color(0xFF00E676) else Color(0xFFFF9800),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Rastreando placas..." else "Pausado",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onTorchToggle,
            modifier = Modifier
                .background(Color(0xAA1E1E1E), CircleShape)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
        ) {
            Icon(
                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                tint = if (isTorchEnabled) Color(0xFFFFEB3B) else Color.White
            )
        }
    }
}

@Composable
fun PlateResultCard(
    plateText: String,
    isLandscape: Boolean = false,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.then(
            if (isLandscape) Modifier.width(310.dp) else Modifier.fillMaxWidth()
        ),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(if (isLandscape) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PLACA DETECTADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAAAAA),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFF2B2B2B), RoundedCornerShape(10.dp))
                    .padding(vertical = if (isLandscape) 10.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = plateText,
                    fontSize = if (isLandscape) 26.sp else 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.5.sp,
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reintentar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar", fontSize = 13.sp)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirmar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun LprCameraScreenPreview() {
    LprCameraScreen(
        uiState = LprCameraUiState(
            detection = PlateDetection(
                plateNumber = "P 123-456",
                boundingBox = AndroidRect(100, 200, 500, 350),
                imageWidth = 720,
                imageHeight = 1280,
                rotationDegrees = 0
            ),
            isScanning = true,
            isTorchEnabled = false
        )
    )
}