package com.example.facedetection.camera

import com.google.mlkit.vision.facemesh.FaceMeshPoint

class NoseReshaper {

    // Các chỉ số cánh mũi (wings) và lỗ mũi
    private val leftNoseIndices = intArrayOf(64, 98, 97, 102, 129, 209, 217, 219, 48, 49, 203, 206, 92, 165)
    private val rightNoseIndices = intArrayOf(294, 327, 326, 331, 358, 429, 437, 439, 278, 279, 423, 426, 322, 391)
    
    // Vùng chóp mũi (tip)
    private val noseTipIndices = intArrayOf(1, 2, 5, 4, 45, 275)

    fun applyReshape(
        allPoints: List<FaceMeshPoint>,
        px: FloatArray,
        py: FloatArray,
        intensity: Float
    ) {
        if (intensity == 0f) return

        // 1. Điểm mốc tham chiếu
        val bridgePoint = allPoints[168] // Điểm giữa hai mắt (Top of bridge)
        val bridgeX = bridgePoint.position.x
        val bridgeY = bridgePoint.position.y
        
        val tipPoint = allPoints[1] // Điểm chóp mũi chính diện
        val originalNoseHeight = tipPoint.position.y - bridgeY

        if (originalNoseHeight <= 0) return

        // Gộp tất cả các điểm mũi cần xử lý
        val allNoseIndices = leftNoseIndices + rightNoseIndices + noseTipIndices

        for (idx in allNoseIndices) {
            if (idx >= px.size || idx >= py.size) continue

            // --- LOGIC TRỤC X: THON GỌN (SLIMMING) ---
            val dx = px[idx] - bridgeX
            // Bóp ngang: Di chuyển các điểm về phía sống mũi
            px[idx] -= dx * (intensity * 0.25f)

            // --- LOGIC TRỤC Y: NÂNG CAO & THU NGẮN (LIFTING) ---
            val dy = py[idx] - bridgeY
            
            if (dy > 0) {
                /**
                 * Thuật toán Lift:
                 * Đẩy các điểm vùng dưới mũi lên phía trên (giảm Y).
                 * Điểm càng xa sống mũi (càng về phía chóp mũi/cánh mũi) thì bị đẩy lên càng nhiều.
                 */
                val liftWeight = (dy / originalNoseHeight).coerceIn(0f, 1.2f)

                // Đẩy lên trên (giảm Y) để tạo hiệu ứng mũi cao/ngắn
                // Cường độ 0.15f giúp mũi trông thanh thoát mà không bị hếch quá mức
                py[idx] -= originalNoseHeight * (intensity * 0.12f * liftWeight)
            }
        }
        
        // Đặc biệt xử lý thêm cho vùng chóp mũi để tạo độ nhọn V-line cho mũi
        for (idx in noseTipIndices) {
            if (idx >= px.size) continue
            val dx = px[idx] - bridgeX
            px[idx] -= dx * (intensity * 0.15f) // Bóp nhọn thêm chóp mũi
        }
    }
}
