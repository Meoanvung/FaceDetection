package com.example.facedetection.camera

import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.abs

class ChinReshaper {

    fun applyReshape(
        allPoints: List<FaceMeshPoint>,
        px: FloatArray,
        py: FloatArray,
        intensity: Float
    ) {
        if (intensity <= 0f) return

        // 1. Xác định trục giữa và kích thước khuôn mặt dựa trên các mốc giải phẫu
        val noseTip = allPoints.find { it.index == 1 } ?: return
        val midX = noseTip.position.x
        val noseY = noseTip.position.y
        
        val chinPoint = allPoints.find { it.index == 152 } ?: return
        val chinY = chinPoint.position.y
        
        val faceHeight = chinY - noseY
        if (faceHeight <= 0) return

        // 2. Duyệt qua TẤT CẢ các điểm để áp dụng biến dạng mượt mà (Global Deformation)
        // Thay vì bóp từng điểm lẻ tẻ, ta bóp "nguyên mảng" theo tỷ lệ khoảng cách đến tâm
        for (i in 0 until px.size) {
            val currentY = py[i]
            
            // Chỉ tác động từ vùng dưới mắt/mũi trở xuống (vùng hạ khuôn mặt)
            if (currentY > noseY - (faceHeight * 0.1f)) {
                
                // Trọng số theo chiều dọc (Vertical Weight):
                // Mạnh nhất ở vùng hàm (0.6 - 0.8), giảm dần về phía mũi và phía cằm
                val relativeY = (currentY - noseY) / faceHeight
                val verticalWeight = when {
                    relativeY < 0f -> 0f
                    relativeY < 0.7f -> (relativeY + 0.1f) / 0.8f // Tăng dần từ mũi xuống
                    else -> 1.0f - (relativeY - 0.7f) * 1.5f // Giảm nhẹ khi xuống sát chóp cằm để giữ dáng V
                }.coerceIn(0f, 1f)

                // Trọng số theo chiều ngang (Horizontal Weight):
                // Điểm càng nằm xa trục giữa (phần thịt má, xương hàm ngoài) càng bị bóp mạnh hơn.
                // Điều này giúp "cắt bớt" phần thịt dày ở hai bên mà không làm méo miệng.
                val dx = px[i] - midX
                val horizontalWeight = (abs(dx) / (faceHeight * 0.8f)).coerceIn(0.2f, 1.0f)

                // Cường độ bóp tổng hợp (Squeeze factor)
                // intensity * 0.25f là ngưỡng an toàn để không làm biến dạng quá mức
                val squeezeFactor = intensity * 0.22f * verticalWeight * horizontalWeight
                
                // Thực hiện bóp: Thu hẹp khoảng cách điểm về phía trục giữa (midX)
                px[i] = midX + dx * (1.0f - squeezeFactor)
                
                // Hiệu ứng "nâng cơ" (Lifting): Đẩy nhẹ các điểm lên trên để tạo cảm giác mặt thanh thoát
                py[i] = currentY - (intensity * 12f * verticalWeight)
            }
        }
    }
}
