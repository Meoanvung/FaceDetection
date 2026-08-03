package com.example.facedetection.camera

import android.graphics.*
import com.google.mlkit.vision.facemesh.FaceMesh

class FaceMeshWarper {

    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    // Các điểm biên của khuôn mặt (Face Oval)
    private val ovalIndices = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378,
        152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21
    )

    /**
     * Biến dạng toàn bộ bức ảnh (Full Frame Warp) dựa trên Face Mesh.
     * Cách này giúp phần hậu cảnh, tóc và cổ tự động co giãn theo khuôn mặt,
     * loại bỏ hoàn toàn hiện tượng "bóng ma" (ghosting) mà không tạo vết mờ.
     */
    fun drawFullWarpedImage(
        canvas: Canvas,
        bitmap: Bitmap,
        mesh: FaceMesh,
        morphedX: FloatArray,
        morphedY: FloatArray,
        viewWidth: Float,
        viewHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        isMirror: Boolean
    ) {
        val allPoints = mesh.allPoints
        val triangles = mesh.allTriangles
        
        val faceVertCount = allPoints.size
        val boundaryVertCount = 8
        val totalVertCount = faceVertCount + boundaryVertCount
        
        val verts = FloatArray(totalVertCount * 2)
        val texs = FloatArray(totalVertCount * 2)
        
        // 1. Gán tọa độ cho Face Mesh
        for (point in allPoints) {
            val i = point.index
            if (i < faceVertCount) {
                verts[i * 2] = morphedX[i]
                verts[i * 2 + 1] = morphedY[i]
                texs[i * 2] = point.position.x
                texs[i * 2 + 1] = point.position.y
            }
        }
        
        // 2. Tạo 8 điểm biên ở rìa ảnh (4 góc + 4 cạnh) để bao phủ toàn bộ Frame
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val boundaryTexs = arrayOf(
            PointF(0f, 0f), PointF(bw / 2f, 0f), PointF(bw, 0f),
            PointF(bw, bh / 2f), PointF(bw, bh), PointF(bw / 2f, bh),
            PointF(0f, bh), PointF(0f, bh / 2f)
        )
        
        for (i in 0 until boundaryVertCount) {
            val idx = faceVertCount + i
            val tp = boundaryTexs[i]
            
            texs[idx * 2] = tp.x
            texs[idx * 2 + 1] = tp.y
            
            var vx = tp.x * scale + offsetX
            if (isMirror) vx = viewWidth - vx
            val vy = tp.y * scale + offsetY
            
            verts[idx * 2] = vx
            verts[idx * 2 + 1] = vy
        }
        
        // 3. Xây dựng danh sách tam giác (Face Triangles + Boundary Triangles)
        val faceTriIndices = ShortArray(triangles.size * 3)
        for (i in triangles.indices) {
            val tri = triangles[i]
            faceTriIndices[i * 3] = tri.allPoints[0].index.toShort()
            faceTriIndices[i * 3 + 1] = tri.allPoints[1].index.toShort()
            faceTriIndices[i * 3 + 2] = tri.allPoints[2].index.toShort()
        }
        
        // Tạo các tam giác nối từ biên mặt (Oval) ra biên ảnh
        val ovalTriIndices = mutableListOf<Short>()
        val ovalSize = ovalIndices.size
        for (i in 0 until ovalSize) {
            val p1 = ovalIndices[i].toShort()
            val p2 = ovalIndices[(i + 1) % ovalSize].toShort()
            
            val sector = (i * boundaryVertCount / ovalSize)
            val nextSector = ((i + 1) * boundaryVertCount / ovalSize) % boundaryVertCount
            
            val b1 = (faceVertCount + sector).toShort()
            
            // Triangle 1
            ovalTriIndices.add(p1); ovalTriIndices.add(p2); ovalTriIndices.add(b1)
            
            // Nếu chuyển sang sector mới, tạo thêm tam giác để nối các điểm biên ảnh
            if (sector != nextSector) {
                val b2 = (faceVertCount + nextSector).toShort()
                ovalTriIndices.add(p2); ovalTriIndices.add(b1); ovalTriIndices.add(b2)
            }
        }
        
        val allIndices = ShortArray(faceTriIndices.size + ovalTriIndices.size)
        System.arraycopy(faceTriIndices, 0, allIndices, 0, faceTriIndices.size)
        for (i in ovalTriIndices.indices) {
            allIndices[faceTriIndices.size + i] = ovalTriIndices[i]
        }
        
        // 4. Vẽ duy nhất một lần toàn bộ Frame đã biến dạng
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            verts.size,
            verts, 0,
            texs, 0,
            null, 0,
            allIndices, 0,
            allIndices.size,
            paint
        )
    }
}
