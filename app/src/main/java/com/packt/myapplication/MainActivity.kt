package com.packt.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var resetButton: Button
    private lateinit var clearLastButton: Button
    private lateinit var captureButton: Button

    private val anchors = mutableListOf<Anchor>()
    private val anchorNodes = mutableListOf<AnchorNode>()
    private val textNodes = mutableListOf<AnchorNode>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        resetButton = findViewById(R.id.resetButton)
        clearLastButton = findViewById(R.id.clearLastButton)
        captureButton = findViewById(R.id.captureButton)

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (plane.type == Plane.Type.VERTICAL || plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                handleTap(hitResult)
            } else {
                Toast.makeText(this, "Выберите вертикальную или горизонтальную плоскость.", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            clearAllAnchors()
            Toast.makeText(this, "Все точки сброшены.", Toast.LENGTH_SHORT).show()
        }

        clearLastButton.setOnClickListener {
            clearLastAnchor()
        }

        captureButton.setOnClickListener {
            captureScreenshot()
        }
    }

    private fun handleTap(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        anchors.add(anchor)

        addMarker(anchor)

        if (anchors.size > 1) {
            val lastIndex = anchors.size - 1
            val distance = calculateDistance(anchors[lastIndex - 1], anchors[lastIndex])
            val midPoint = calculateMidPoint(anchors[lastIndex - 1], anchors[lastIndex])
            Toast.makeText(this, "Расстояние: ${"%.2f".format(distance)} м", Toast.LENGTH_SHORT).show()
            showDistanceText(midPoint, distance)
        }
    }

    private fun addMarker(anchor: Anchor) {
        val anchorNode = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
        }

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.BLUE))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.01f, Vector3.zero(), material)
                anchorNode.renderable = sphere
            }

        anchorNodes.add(anchorNode)
    }

    private fun showDistanceText(position: Vector3, distance: Double) {
        ViewRenderable.builder()
            .setView(this, R.layout.distance_text_view)
            .build()
            .thenAccept { renderable ->

                val textNode = AnchorNode().apply {
                    worldPosition = position
                }
                textNode.renderable = renderable


                val textView = renderable.view.findViewById<TextView>(R.id.distanceTextView)
                textView.text = "${"%.2f".format(distance)} м"


                arFragment.arSceneView.scene.addChild(textNode)
                textNodes.add(textNode)


                arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
                    val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
                    val directionToCamera = Vector3.subtract(cameraPosition, textNode.worldPosition).normalized()

                    val distanceToCamera = Vector3.subtract(textNode.worldPosition, cameraPosition).length()


                    val scaleFactor = distanceToCamera / 2.0f
                    textNode.localScale = Vector3(scaleFactor, scaleFactor, scaleFactor)


                    textNode.worldRotation = Quaternion.lookRotation(directionToCamera, Vector3.up())
                }
            }
            .exceptionally { throwable ->
                Log.e("AR_TEXT", "Не удалось создать текстовый элемент: ${throwable.message}")
                null
            }
    }


    private fun clearAllAnchors() {

        anchors.forEach { it.detach() }
        anchors.clear()


        anchorNodes.forEach { it.setParent(null) }
        anchorNodes.clear()


        textNodes.forEach { it.setParent(null) }
        textNodes.clear()
    }

    private fun clearLastAnchor() {
        if (anchors.isNotEmpty()) {
            anchors.removeAt(anchors.size - 1).detach()

            if (anchorNodes.isNotEmpty()) {
                anchorNodes.removeAt(anchorNodes.size - 1).setParent(null)
            }

            if (textNodes.isNotEmpty()) {
                textNodes.removeAt(textNodes.size - 1).setParent(null)
            }

            Toast.makeText(this, "Последняя точка удалена.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нет точек для удаления.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateMidPoint(anchor1: Anchor, anchor2: Anchor): Vector3 {
        val pose1 = anchor1.pose
        val pose2 = anchor2.pose

        val midX = (pose1.tx() + pose2.tx()) / 2
        val midY = (pose1.ty() + pose2.ty()) / 2
        val midZ = (pose1.tz() + pose2.tz()) / 2

        return Vector3(midX, midY, midZ)
    }

    private fun calculateDistance(anchor1: Anchor, anchor2: Anchor): Double {
        val pose1 = anchor1.pose
        val pose2 = anchor2.pose

        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()

        return kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    private fun captureScreenshot() {
        val bitmap = Bitmap.createBitmap(
            arFragment.arSceneView.width,
            arFragment.arSceneView.height,
            Bitmap.Config.ARGB_8888
        )

        PixelCopy.request(
            arFragment.arSceneView,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    saveBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Ошибка при создании фото", Toast.LENGTH_SHORT).show()
                }
            },
            android.os.Handler(mainLooper)
        )
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val filename = "AR_Screenshot_${System.currentTimeMillis()}.png"
        val path = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.absolutePath
        val file = File(path, filename)

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Toast.makeText(this, "Фото сохранено: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Не удалось сохранить фото", Toast.LENGTH_SHORT).show()
        }
    }
}
