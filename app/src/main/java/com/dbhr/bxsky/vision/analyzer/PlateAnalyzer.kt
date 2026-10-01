package com.dbhr.bxsky.vision.analyzer

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.dbhr.bxsky.core.utils.PlateValidator
import com.dbhr.bxsky.data.model.PlateDetection
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class PlateAnalyzer(
    private val onPlateDetected: (PlateDetection?) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    var foundDetection: PlateDetection? = null

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val validPlate = PlateValidator.extractValidPlate(line.text)
                            val box = line.boundingBox

                            if (validPlate != null && box != null) {
                                foundDetection = PlateDetection(
                                    plateNumber = validPlate,
                                    boundingBox = box,
                                    imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width,
                                    imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height,
                                    rotationDegrees = rotationDegrees
                                )
                                break
                            }
                        }
                        if (foundDetection != null) break
                    }

                    onPlateDetected(foundDetection)
                }
                .addOnFailureListener { exc ->
                    Log.e("PlateAnalyzer", "Error en ML Kit: ${exc.message}")
                    onPlateDetected(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("PlateAnalyzer", "Excepción al procesar imagen: ${e.message}", e)
            imageProxy.close()
        }
    }
}