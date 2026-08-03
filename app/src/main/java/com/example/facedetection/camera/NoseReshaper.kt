package com.example.facedetection.camera

import com.google.mlkit.vision.facemesh.FaceMeshPoint

class NoseReshaper {

    // Indices for nose wings (alar) and nostrils area in 468 points topology
    private val leftNoseIndices = intArrayOf(
        64, 98, 97, 102, 129, 209, 217, 219, 48, 49, 203, 206, 92, 165
    )

    private val rightNoseIndices = intArrayOf(
        294, 327, 326, 331, 358, 429, 437, 439, 278, 279, 423, 426, 322, 391
    )
    
    // Nose tip area
    private val noseTipIndices = intArrayOf(1, 2, 5, 4, 45, 275)

    fun applyReshape(
        allPoints: List<FaceMeshPoint>,
        px: FloatArray,
        py: FloatArray,
        intensity: Float
    ) {
        if (intensity == 0f) return

        // Use the nose bridge as center reference (e.g., point 6 or 168)
        val bridgeCenterX = allPoints[6].position.x

        // Slimming logic: move wing points horizontally toward the center
        for (idx in leftNoseIndices) {
            if (idx >= px.size) continue
            val dist = px[idx] - bridgeCenterX
            px[idx] -= dist * intensity
        }

        for (idx in rightNoseIndices) {
            if (idx >= px.size) continue
            val dist = px[idx] - bridgeCenterX
            px[idx] -= dist * intensity
        }
        
        // Optional: Sharpen tip by moving points slightly closer to center
        for (idx in noseTipIndices) {
            if (idx >= px.size) continue
            val dist = px[idx] - bridgeCenterX
            px[idx] -= dist * (intensity * 0.3f)
        }
    }
}
