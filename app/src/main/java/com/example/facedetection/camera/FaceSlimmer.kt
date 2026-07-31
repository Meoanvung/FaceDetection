package com.example.facedetection.camera

import com.google.mlkit.vision.facemesh.FaceMeshPoint

class FaceSlimmer {

    // Jawline indices from left to right (0 to 16 sequence in some topologies, 
    // but in 468 points, they are often scattered)
    // Using standard mapping for jawline and chin area
    private val leftJawIndices = intArrayOf(234, 93, 132, 58, 172, 136, 150, 149, 176, 148)
    private val rightJawIndices = intArrayOf(454, 323, 361, 288, 397, 365, 379, 378, 400, 377)
    private val chinIndex = 152
    
    // Cheek/Jaw angle indices for sharpening
    private val cheekIndicesLeft = intArrayOf(205, 203, 98, 97)
    private val cheekIndicesRight = intArrayOf(425, 423, 327, 326)

    /**
     * intensity > 0: slims and sharpens the face
     */
    fun applyReshape(
        allPoints: List<FaceMeshPoint>,
        px: FloatArray,
        py: FloatArray,
        intensity: Float
    ) {
        if (intensity == 0f) return

        // Vertical center line based on nose bridge (6) and chin (152)
        val centerX = (allPoints[6].position.x + allPoints[152].position.x) / 2f
        val topHeadY = allPoints[10].position.y
        val chinY = allPoints[152].position.y
        val faceHeight = chinY - topHeadY

        // 1. Narrow the Jawline (Move inward horizontally)
        for (idx in leftJawIndices) {
            if (idx >= px.size) continue
            val dist = px[idx] - centerX
            px[idx] -= dist * intensity * 0.8f
        }
        for (idx in rightJawIndices) {
            if (idx >= px.size) continue
            val dist = px[idx] - centerX
            px[idx] -= dist * intensity * 0.8f
        }

        // 2. Sharpen Cheeks (Move inward and slightly up)
        for (idx in cheekIndicesLeft) {
            if (idx >= px.size) continue
            px[idx] -= (px[idx] - centerX) * intensity * 0.5f
            py[idx] -= faceHeight * intensity * 0.05f
        }
        for (idx in cheekIndicesRight) {
            if (idx >= px.size) continue
            px[idx] -= (px[idx] - centerX) * intensity * 0.5f
            py[idx] -= faceHeight * intensity * 0.05f
        }

        // 3. Lengthen the Chin (Move chin tip downward)
        if (chinIndex < py.size) {
            py[chinIndex] += faceHeight * intensity * 0.1f
        }
    }
}
