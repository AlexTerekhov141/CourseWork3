package com.packt.myapplication.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.packt.myapplication.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MainActivity2 : AppCompatActivity() {
    private var sphereRadius = 0.5f
    private lateinit var sceneView: SceneView
    private lateinit var transformationSystem: TransformationSystem
    private lateinit var resetButton: Button
    private var modelNode: TransformableNode? = null
    private val points = mutableListOf<Vector3>()
    private var downloadUrl: String = ""
    private val anchorNodes = mutableListOf<TransformableNode>()
    private var calibrationFactor: Float = 1.0f
    private var isCalibrationMode: Boolean = false
    private var calibrationPoint1: Vector3? = null
    private var calibrationPoint2: Vector3? = null
    private val calibrationNodes = mutableListOf<TransformableNode>()
    private lateinit var progressBar: ProgressBar

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        sceneView = findViewById(R.id.sceneView)
        progressBar = findViewById(R.id.progressBar)
        transformationSystem = TransformationSystem(
            resources.displayMetrics,
            FootprintSelectionVisualizer()
        )

        val projectName = intent.getStringExtra("projectName") ?: ""
        if (projectName.isEmpty()) {
            Toast.makeText(this, "Имя проекта не указано", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        downloadUrl = "http://188.245.194.36:5002/download2?projectName=$projectName"

        downloadGlbFile()

        setupButtons()

        sceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleTouch(event)
                return@setOnTouchListener true
            }
            false
        }
    }

    private fun handleTouch(event: MotionEvent) {
        val hitTestResult = sceneView.scene.hitTestAll(event)
        if (hitTestResult.isNotEmpty()) {
            val hit = hitTestResult[0]
            val hitPose = hit.point

            if (isCalibrationMode) {
                if (calibrationPoint1 == null) {
                    calibrationPoint1 = hitPose
                    addCalibrationMarker(hitPose, android.graphics.Color.RED)
                    Toast.makeText(this, "Первая калибровочная точка установлена. Нажмите на вторую точку.", Toast.LENGTH_SHORT).show()
                } else if (calibrationPoint2 == null) {
                    calibrationPoint2 = hitPose
                    addCalibrationMarker(hitPose, android.graphics.Color.RED)
                    val measuredCalibrationDistance = calculateDistance(calibrationPoint1!!, calibrationPoint2!!)
                    promptForRealDistance(measuredCalibrationDistance)
                }
                return
            }

            addMarker(hitPose, android.graphics.Color.BLUE)
            if (points.isNotEmpty()) {
                val previousPoint = points.last()
                val distance = calculateDistance(previousPoint, hitPose)
                showDistance(distance)
            }
            points.add(hitPose)
        }
    }

    private fun addMarker(position: Vector3, color: Int) {
        MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(color))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.01f, Vector3.zero(), material)
                val markerNode = TransformableNode(transformationSystem).apply {
                    worldPosition = position
                    renderable = sphere
                }
                sceneView.scene.addChild(markerNode)
                anchorNodes.add(markerNode)

                updateMarkerScale(markerNode)

                markerNode.setOnTapListener { _, _ ->
                    updateMarkerScale(markerNode)
                }
            }
            .exceptionally {
                Toast.makeText(this, "Ошибка создания маркера", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun updateMarkerScale(markerNode: TransformableNode) {
        val cameraPosition = sceneView.scene.camera.worldPosition
        val markerPosition = markerNode.worldPosition
        val distance = Vector3.subtract(cameraPosition, markerPosition).length()

        val scaleFactor = distance * 0.01f
        markerNode.localScale = Vector3.one().scaled(scaleFactor)
    }


    private fun addCalibrationMarker(position: Vector3, color: Int) {
        MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(color))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.01f, Vector3.zero(), material)
                val markerNode = TransformableNode(transformationSystem).apply {
                    worldPosition = position
                    renderable = sphere
                }
                sceneView.scene.addChild(markerNode)
                calibrationNodes.add(markerNode)
            }
            .exceptionally {
                Toast.makeText(this, "Ошибка создания калибровочного маркера", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun calculateDistance(point1: Vector3, point2: Vector3): Float {
        return Vector3.subtract(point1, point2).length()
    }

    private fun showDistance(distance: Float) {
        val calibratedDistance = distance * calibrationFactor
        runOnUiThread {
                Toast.makeText(
                    this,
                    "distance: %.2f м".format( calibratedDistance),
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun setupButtons() {
        val btnUp = findViewById<Button>(R.id.btnUp)
        val btnDown = findViewById<Button>(R.id.btnDown)
        val btnLeft = findViewById<Button>(R.id.btnLeft)
        val btnRight = findViewById<Button>(R.id.btnRight)
        val btnRotateLeft = findViewById<Button>(R.id.btnRotateLeft)
        val btnRotateRight = findViewById<Button>(R.id.btnRotateRight)
        val btnRotateUP = findViewById<Button>(R.id.btnRotateUp)
        val btnRotateDown = findViewById<Button>(R.id.btnRotateDown)
        val btnScaleUp = findViewById<Button>(R.id.ScaleUp)
        val btnScaleDown = findViewById<Button>(R.id.ScaleDown)
        resetButton = findViewById(R.id.resetButton)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val calibrateButton = findViewById<Button>(R.id.calibrateButton)

        btnUp.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x, pos.y + 0.1f, pos.z)
            }
        }
        btnDown.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x, pos.y - 0.1f, pos.z)
            }
        }
        btnLeft.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x - 0.1f, pos.y, pos.z)
            }
        }
        btnRight.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x + 0.1f, pos.y, pos.z)
            }
        }
        btnRotateLeft.setOnClickListener {
            modelNode?.let {
                val currentRotation = it.worldRotation
                val delta = Quaternion.axisAngle(Vector3(0f, 1f, 0f), -10f)
                it.worldRotation = Quaternion.multiply(currentRotation, delta)
            }
        }
        btnRotateRight.setOnClickListener {
            modelNode?.let {
                val currentRotation = it.worldRotation
                val delta = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 10f)
                it.worldRotation = Quaternion.multiply(currentRotation, delta)
            }
        }
        btnRotateUP.setOnClickListener {
            modelNode?.let {
                val currentRotation = it.worldRotation
                val delta = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -10f)
                it.worldRotation = Quaternion.multiply(currentRotation, delta)
            }
        }
        btnRotateDown.setOnClickListener {
            modelNode?.let {
                val currentRotation = it.worldRotation
                val delta = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 10f)
                it.worldRotation = Quaternion.multiply(currentRotation, delta)
            }
        }
        btnScaleUp.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x, pos.y, pos.z + 0.1f)
            }
        }
        btnScaleDown.setOnClickListener {
            modelNode?.let {
                val pos = it.worldPosition
                it.worldPosition = Vector3(pos.x, pos.y, pos.z - 0.1f)
            }
        }

        btnShare.setOnClickListener {
            shareGlbFile()
        }
        resetButton.setOnClickListener {
            clearLastAnchor()
        }
        calibrateButton.setOnClickListener {
            startCalibration()
        }
    }

    private fun clearLastAnchor() {
        if (anchorNodes.isNotEmpty()) {
            val lastNode = anchorNodes.removeAt(anchorNodes.size - 1)
            lastNode.setParent(null)
            if (points.isNotEmpty()) {
                points.removeAt(points.size - 1)
            }
            Toast.makeText(this, getString(R.string.LastDotClearToast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.NotDotsToClearToast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCalibration() {
        isCalibrationMode = true
        calibrationPoint1 = null
        calibrationPoint2 = null
        Toast.makeText(this, "Калибровка: нажмите на первую точку", Toast.LENGTH_SHORT).show()
    }

    private fun promptForRealDistance(measuredDistance: Float) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Введите реальное расстояние")
        builder.setMessage("Измеренное расстояние: %.2f м. Введите реальное значение (в метрах):".format(measuredDistance))
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val realDistance = input.text.toString().toFloatOrNull()
            if (realDistance != null && measuredDistance > 0f) {
                calibrationFactor = realDistance / measuredDistance
                isCalibrationMode = false
                calibrationNodes.forEach { node ->
                    node.setParent(null)
                }
                calibrationNodes.clear()
                Toast.makeText(this, "Калибровка завершена. Коэффициент: %.2f".format(calibrationFactor), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Неверное значение", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
            isCalibrationMode = false
            Toast.makeText(this, "Калибровка отменена", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun shareGlbFile() {
        val glbFile = File(filesDir, "3d_model.glb")
        if (!glbFile.exists()) {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", glbFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "model/gltf-binary"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться .glb"))
    }

    private fun downloadGlbFile() {
        runOnUiThread { progressBar.visibility = View.VISIBLE }
        val client = OkHttpClient()
        val request = Request.Builder().url(downloadUrl).build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("Ошибка скачивания")
                val glbFile = File(filesDir, "3d_model.glb")
                FileOutputStream(glbFile).use { fos ->
                    fos.write(response.body?.bytes())
                }
                runOnUiThread {
                    loadObjModel(glbFile)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun loadObjModel(objFile: File) {
        val renderableSource = RenderableSource.builder()
            .setSource(this, objFile.toUri(), RenderableSource.SourceType.GLB)
            .setScale(0.2f)
            .setRecenterMode(RenderableSource.RecenterMode.ROOT)
            .build()

        ModelRenderable.builder()
            .setSource(this, renderableSource)
            .setRegistryId(objFile)
            .build()
            .thenAccept { renderable ->
                modelNode = TransformableNode(transformationSystem).apply {
                    this.renderable = renderable
                    worldPosition = Vector3(0f, 0f, -0.4f)
                }
                sceneView.scene.addChild(modelNode)
                modelNode?.select()
            }
            .exceptionally {
                Toast.makeText(this, "Ошибка загрузки .glb", Toast.LENGTH_SHORT).show()
                null
            }
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
    }
}
