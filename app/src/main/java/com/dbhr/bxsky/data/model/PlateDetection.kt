package com.dbhr.bxsky.data.model

import android.graphics.Rect

data class PlateDetection(
    val plateNumber: String,
    val boundingBox: Rect,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int
)