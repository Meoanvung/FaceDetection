package com.example.facedetection.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.facemesh.FaceMesh

class FaceMeshOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 200
    }

    private var faceMesh: FaceMesh? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var isMirror: Boolean = false
    
    private val eyeReshaper = EyeReshaper()
    private val noseReshaper = NoseReshaper()
    private val faceSlimmer = FaceSlimmer()

    var lipReshapeIntensity: Float = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    var eyeReshapeIntensity: Float = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    var noseReshapeIntensity: Float = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    var faceSlimIntensity: Float = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    private val upperLipIndices = intArrayOf(
        0, 37, 39, 40, 61, 267, 269, 270, 291,
        78, 80, 81, 82, 13, 312, 311, 310, 308, 191, 409, 415
    )

    private val lowerLipIndices = intArrayOf(
        17, 84, 91, 146, 178, 181, 314, 317, 318, 321, 325, 375, 402, 405,
        95, 88, 14
    )

    fun updateFaceMesh(mesh: FaceMesh?, width: Int, height: Int, mirror: Boolean) {
        this.faceMesh = mesh
        this.imageWidth = width
        this.imageHeight = height
        this.isMirror = mirror
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mesh = faceMesh ?: return
        if (imageWidth == 0 || imageHeight == 0) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight)
        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f

        val allPoints = mesh.allPoints
        if (allPoints.isEmpty()) return

        val lipCenterY = (allPoints[13].position.y + allPoints[14].position.y) / 2f

        // Prepare arrays with original image coordinates
        val pointsX = FloatArray(allPoints.size)
        val pointsY = FloatArray(allPoints.size)
        for (i in allPoints.indices) {
            pointsX[i] = allPoints[i].position.x
            pointsY[i] = allPoints[i].position.y
        }

        // 1. Apply Lip Reshaping (Logic inside Overlay)
        for (i in allPoints.indices) {
            val idx = allPoints[i].index
            val dist = pointsY[i] - lipCenterY
            if (upperLipIndices.contains(idx) || lowerLipIndices.contains(idx)) {
                pointsY[i] += dist * lipReshapeIntensity
            }
        }

        // 2. Apply Eye Reshaping (Calling separate file logic)
        eyeReshaper.applyReshape(allPoints, pointsX, pointsY, eyeReshapeIntensity)

        // 3. Apply Nose Reshaping (Calling separate file logic)
        noseReshaper.applyReshape(allPoints, pointsX, pointsY, noseReshapeIntensity)

        // 4. Apply Face Slimming (Calling separate file logic)
        faceSlimmer.applyReshape(allPoints, pointsX, pointsY, faceSlimIntensity)

        // 5. Scale and Draw
        val tx = FloatArray(allPoints.size)
        val ty = FloatArray(allPoints.size)
        for (i in allPoints.indices) {
            var finalX = pointsX[i] * scale + offsetX
            if (isMirror) finalX = viewWidth - finalX
            val finalY = pointsY[i] * scale + offsetY
            tx[i] = finalX
            ty[i] = finalY
        }

        for (triangle in mesh.allTriangles) {
            val i1 = allPoints.indexOf(triangle.allPoints[0])
            val i2 = allPoints.indexOf(triangle.allPoints[1])
            val i3 = allPoints.indexOf(triangle.allPoints[2])

            if (i1 != -1 && i2 != -1 && i3 != -1) {
                canvas.drawLine(tx[i1], ty[i1], tx[i2], ty[i2], linePaint)
                canvas.drawLine(tx[i2], ty[i2], tx[i3], ty[i3], linePaint)
                canvas.drawLine(tx[i3], ty[i3], tx[i1], ty[i1], linePaint)
            }
        }
    }
}
