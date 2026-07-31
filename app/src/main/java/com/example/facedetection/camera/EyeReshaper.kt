package com.example.facedetection.camera

import android.graphics.PointF
import com.google.mlkit.vision.facemesh.FaceMeshPoint

class EyeReshaper {

    private val leftEyeIndices = intArrayOf(
        33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246,
        130, 247, 30, 29, 27, 28, 56, 190, 243, 112, 26, 22, 23, 24, 110, 25
    )

    private val rightEyeIndices = intArrayOf(
        362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398,
        359, 467, 260, 259, 257, 258, 286, 414, 463, 341, 256, 252, 253, 254, 339, 255
    )

    fun applyReshape(
        allPoints: List<FaceMeshPoint>,
        pointsX: FloatArray,
        pointsY: FloatArray,
        intensity: Float
    ) {
        if (intensity == 0f) return

        // Calculate Centers in image coordinates
        val leftCenter = getCenter(allPoints, intArrayOf(159, 145, 33, 133))
        val rightCenter = getCenter(allPoints, intArrayOf(386, 374, 362, 263))

        // Apply deformation
        reshapeEye(pointsX, pointsY, leftEyeIndices, leftCenter, intensity)
        reshapeEye(pointsX, pointsY, rightEyeIndices, rightCenter, intensity)
    }

    private fun getCenter(allPoints: List<FaceMeshPoint>, indices: IntArray): PointF {
        var sumX = 0f
        var sumY = 0f
        for (idx in indices) {
            sumX += allPoints[idx].position.x
            sumY += allPoints[idx].position.y
        }
        return PointF(sumX / indices.size, sumY / indices.size)
    }

    private fun reshapeEye(
        px: FloatArray,
        py: FloatArray,
        indices: IntArray,
        center: PointF,
        intensity: Float
    ) {
        for (idx in indices) {
            if (idx >= px.size) continue
            
            val dx = px[idx] - center.x
            val dy = py[idx] - center.y
            
            // intensity > 0 makes dx, dy larger (enlarge)
            // intensity < 0 makes dx, dy smaller (shrink)
            px[idx] = center.x + dx * (1f + intensity)
            py[idx] = center.y + dy * (1f + intensity)
        }
    }
}
