package com.example.facedetection.camera

import com.google.mlkit.vision.facemesh.FaceMeshPoint

class FaceMorpher {
    private val eyeReshaper = EyeReshaper()
    private val noseReshaper = NoseReshaper()

    private val upperLipIndices = intArrayOf(0, 37, 39, 40, 61, 267, 269, 270, 291, 78, 80, 81, 82, 13, 312, 311, 310, 308, 191, 409, 415)
    private val lowerLipIndices = intArrayOf(17, 84, 91, 146, 178, 181, 314, 317, 318, 321, 325, 375, 402, 405, 95, 88, 14)

    fun getMorphedPoints(
        allPoints: List<FaceMeshPoint>,
        lipIntensity: Float,
        eyeIntensity: Float,
        noseIntensity: Float
    ): Pair<FloatArray, FloatArray> {
        // Khởi tạo mảng 468 điểm để đảm bảo đúng Index của Face Mesh
        val px = FloatArray(468)
        val py = FloatArray(468)

        // Gán tọa độ gốc vào đúng vị trí Index
        for (point in allPoints) {
            val i = point.index
            if (i < 468) {
                px[i] = point.position.x
                py[i] = point.position.y
            }
        }

        // 1. Morph Lips
        var p13Y = 0f
        var p14Y = 0f
        for (p in allPoints) {
            if (p.index == 13) p13Y = p.position.y
            if (p.index == 14) p14Y = p.position.y
        }
        val lipCenterY = (p13Y + p14Y) / 2f

        for (i in 0 until 468) {
            if (upperLipIndices.contains(i) || lowerLipIndices.contains(i)) {
                py[i] += (py[i] - lipCenterY) * lipIntensity
            }
        }

        // 2. Morph Eyes
        eyeReshaper.applyReshape(allPoints, px, py, eyeIntensity)

        // 3. Morph Nose
        noseReshaper.applyReshape(allPoints, px, py, noseIntensity)

        return Pair(px, py)
    }
}