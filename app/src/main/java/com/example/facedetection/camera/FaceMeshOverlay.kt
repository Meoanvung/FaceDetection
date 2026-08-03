package com.example.facedetection.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.facemesh.FaceMesh

class FaceMeshOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 80
    }

    private var faceMesh: FaceMesh? = null
    private var currentBitmap: Bitmap? = null
    private var isMirror: Boolean = false
    
    var showMesh: Boolean = true
        set(value) { field = value; postInvalidate() }
    
    private val faceMorpher = FaceMorpher()
    private val faceWarper = FaceMeshWarper()

    var lipReshapeIntensity: Float = 0f
        set(value) { field = value; postInvalidate() }
    var eyeReshapeIntensity: Float = 0f
        set(value) { field = value; postInvalidate() }
    var noseReshapeIntensity: Float = 0f
        set(value) { field = value; postInvalidate() }
    var chinReshapeIntensity: Float = 0f
        set(value) { field = value; postInvalidate() }

    fun updateData(mesh: FaceMesh?, bitmap: Bitmap?, mirror: Boolean) {
        this.faceMesh = mesh
        this.currentBitmap = bitmap
        this.isMirror = mirror
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mesh = faceMesh ?: return
        val bitmap = currentBitmap ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scale = Math.max(viewWidth / bitmap.width, viewHeight / bitmap.height)
        val offsetX = (viewWidth - bitmap.width * scale) / 2f
        val offsetY = (viewHeight - bitmap.height * scale) / 2f

        // 1. Lấy tọa độ các điểm đã biến dạng (Morphing)
        val (morphedX, morphedY) = faceMorpher.getMorphedPoints(
            mesh.allPoints,
            lipReshapeIntensity,
            eyeReshapeIntensity,
            noseReshapeIntensity,
            chinReshapeIntensity
        )

        // 2. Chuyển đổi tọa độ sang hệ tọa độ View (Scale & Offset)
        val tx = FloatArray(468)
        val ty = FloatArray(468)

        for (i in 0 until 468) {
            var x = morphedX[i] * scale + offsetX
            if (isMirror) x = viewWidth - x
            val y = morphedY[i] * scale + offsetY
            tx[i] = x
            ty[i] = y
        }

        // 3. Vẽ toàn bộ bức ảnh đã biến dạng (Full Frame Warp)
        // Cách này vẽ toàn bộ frame (bao gồm cả tóc, cổ, nền) qua một lưới biến dạng duy nhất.
        // Điều này đảm bảo khi cằm bóp vào, phần nền xung quanh sẽ "trôi" theo, 
        // không để lại hiện tượng bóng ma hay các vết mờ cục bộ.
        faceWarper.drawFullWarpedImage(
            canvas, bitmap, mesh, tx, ty,
            viewWidth, viewHeight, scale, offsetX, offsetY, isMirror
        )

        // 4. Vẽ lưới (Chỉ vẽ khi showMesh = true)
        if (showMesh) {
            for (triangle in mesh.allTriangles) {
                val i1 = triangle.allPoints[0].index
                val i2 = triangle.allPoints[1].index
                val i3 = triangle.allPoints[2].index
                
                if (i1 < 468 && i2 < 468 && i3 < 468) {
                    canvas.drawLine(tx[i1], ty[i1], tx[i2], ty[i2], linePaint)
                    canvas.drawLine(tx[i2], ty[i2], tx[i3], ty[i3], linePaint)
                    canvas.drawLine(tx[i3], ty[i3], tx[i1], ty[i1], linePaint)
                }
            }
        }
    }
}
