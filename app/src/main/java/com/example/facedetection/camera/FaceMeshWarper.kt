package com.example.facedetection.camera

import android.graphics.*
import com.google.mlkit.vision.facemesh.FaceMesh

class FaceMeshWarper {

    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    fun drawWarpedImage(
        canvas: Canvas,
        bitmap: Bitmap,
        mesh: FaceMesh,
        morphedX: FloatArray,
        morphedY: FloatArray
    ) {
        val allPoints = mesh.allPoints
        val triangles = mesh.allTriangles
        
        // ML Kit Face Mesh tiêu chuẩn có 468 điểm
        val vertCount = 468
        val verts = FloatArray(vertCount * 2)
        val texs = FloatArray(vertCount * 2)
        
        // Gán dữ liệu vào mảng theo đúng index để tránh rách lưới
        for (point in allPoints) {
            val i = point.index
            if (i < vertCount) {
                // Tọa độ đã bóp méo trên View
                verts[i * 2] = morphedX[i]
                verts[i * 2 + 1] = morphedY[i]
                
                // Tọa độ gốc trên Bitmap để lấy pixel
                texs[i * 2] = point.position.x
                texs[i * 2 + 1] = point.position.y
            }
        }
        
        // Chuẩn bị các chỉ số tam giác
        val indices = ShortArray(triangles.size * 3)
        for (i in triangles.indices) {
            val tri = triangles[i]
            // Dùng index thật của điểm để kết nối tam giác chính xác
            indices[i * 3] = tri.allPoints[0].index.toShort()
            indices[i * 3 + 1] = tri.allPoints[1].index.toShort()
            indices[i * 3 + 2] = tri.allPoints[2].index.toShort()
        }
        
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            verts.size,
            verts, 0,
            texs, 0,
            null, 0,
            indices, 0,
            indices.size,
            paint
        )
    }
}