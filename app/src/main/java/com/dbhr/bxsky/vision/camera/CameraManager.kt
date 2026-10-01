package com.dbhr.bxsky.vision.camera

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.dbhr.bxsky.data.model.PlateDetection
import com.dbhr.bxsky.vision.analyzer.PlateAnalyzer
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var camera: Camera? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var orientationEventListener: OrientationEventListener? = null

    fun startCamera(
        previewView: PreviewView,
        onPlateDetected: (PlateDetection?) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()



                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }



                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            cameraExecutor,
                            PlateAnalyzer { detection ->
                                ContextCompat.getMainExecutor(context).execute {
                                    onPlateDetected(detection)
                                }
                            }
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                orientationEventListener = object : OrientationEventListener(context) {
                    override fun onOrientationChanged(orientation: Int) {
                        if (orientation == ORIENTATION_UNKNOWN) return

                        val rotation = when (orientation) {
                            in 45..134 -> Surface.ROTATION_270
                            in 135..224 -> Surface.ROTATION_180
                            in 225..314 -> Surface.ROTATION_90
                            else -> Surface.ROTATION_0
                        }

                        preview.targetRotation = rotation
                        imageAnalyzer.targetRotation = rotation
                    }
                }
                orientationEventListener?.enable()

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("CameraManager", "Fallo al inicializar CameraX: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun shutdown() {
        orientationEventListener?.disable()
        cameraExecutor.shutdown()
    }
}