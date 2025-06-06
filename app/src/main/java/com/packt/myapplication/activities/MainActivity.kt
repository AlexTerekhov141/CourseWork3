package com.packt.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color as androidColor
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.widget.Button
import android.widget.ImageButton
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
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.media.ExifInterface
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.packt.myapplication.R


class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var arFragment: ArFragment
    private lateinit var resetButton: Button
    private lateinit var clearLastButton: Button
    private lateinit var captureButton: Button
    private lateinit var settingsButton : ImageButton
    private val anchors = mutableListOf<Anchor>()
    private val anchorNodes = mutableListOf<AnchorNode>()
    private val textNodes = mutableListOf<AnchorNode>()


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //startCamera()
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        resetButton = findViewById(R.id.resetButton)
        clearLastButton = findViewById(R.id.clearLastButton)
        captureButton = findViewById(R.id.captureButton)
        settingsButton = findViewById(R.id.action_settings)
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (plane.type == Plane.Type.VERTICAL || plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                handleTap(hitResult)
            } else {
                Toast.makeText(this, getString(R.string.ChoosePlateToast), Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            clearAllAnchors()
            Toast.makeText(this, getString(R.string.AllDotsClearToast), Toast.LENGTH_SHORT).show()
        }

        clearLastButton.setOnClickListener {
            clearLastAnchor()
        }
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        captureButton.setOnClickListener {
            captureScreenshot()
            //takePhoto()
        }

        showOnboarding(resetButton, captureButton, clearLastButton)

    }
    private fun isOnboardingShown(): Boolean {
        val sharedPrefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean("onboarding_shown", false)
    }

    private fun setOnboardingShown() {
        val sharedPrefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("onboarding_shown", true).apply()
    }
    private fun showOnboarding(resetButton: Button, captureButton: Button, clearLastButton: Button) {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(resetButton)
            .setPrimaryText(getString(R.string.DotsDropHint))
            .setSecondaryText(getString(R.string.DotsDropHintText))
            .setBackgroundColour(androidColor.parseColor("#80000000"))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    showSecondPrompt(captureButton, clearLastButton)
                }
            }
            .show()
    }

    private fun showSecondPrompt(resetButton: Button, clearLastButton: Button) {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(clearLastButton)
            .setPrimaryText(getString(R.string.ClearLastButtonHint))
            .setSecondaryText(getString(R.string.ClearLastButtonHintText))
            .setBackgroundColour(androidColor.parseColor("#80000000"))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    showThirdPrompt(resetButton)
                }
            }
            .show()
    }
    private fun showThirdPrompt(resetButton: Button) {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(resetButton)
            .setPrimaryText(getString(R.string.TakePhoto))
            .setSecondaryText(getString(R.string.TakePhotoHintText))
            .setBackgroundColour(androidColor.parseColor("#80000000"))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    setOnboardingShown()
                }
            }
            .show()
    }
    private fun handleTap(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        anchors.add(anchor)

        addMarker(anchor)
        val selectedTool = sharedPreferences.getString("selected_tool", "measure")
        if (anchors.size > 1) {
            val lastIndex = anchors.size - 1
            val distance = calculateDistance(anchors[lastIndex - 1], anchors[lastIndex])
            val midPoint = calculateMidPoint(anchors[lastIndex - 1], anchors[lastIndex])
            Toast.makeText(this, "Дистанция: ${"%.2f".format(distance)} м", Toast.LENGTH_SHORT).show()
            if(selectedTool == "measure"){
                showDistanceText(midPoint, distance)
            }else if(selectedTool == "area"){
                if (anchors.size >= 4){
                    val area = calculateArea(anchors)
                    showAreaText(anchors,area)
                }

            }else{
                if(anchors.size >=3){
                    val corner = calculateCorner(anchors)
                    showCornerText(anchors,corner)
                }
            }
        }
    }
    private fun calculateArea(anchors: List<Anchor>): Double {
        val length = calculateDistance(anchors[0], anchors[1])
        val width = calculateDistance(anchors[1], anchors[2])

        return length * width
    }

    private fun calculateCorner(anchors: List<Anchor>): Double {
        val lastIndex = anchors.size - 1
        val pointA = anchors[lastIndex - 2].pose
        val pointB = anchors[lastIndex - 1].pose
        val pointC = anchors[lastIndex].pose

        val vectorAB = Vector3(
            pointB.tx() - pointA.tx(),
            pointB.ty() - pointA.ty(),
            pointB.tz() - pointA.tz()
        )

        val vectorBC = Vector3(
            pointC.tx() - pointB.tx(),
            pointC.ty() - pointB.ty(),
            pointC.tz() - pointB.tz()
        )

        val dotProduct = (vectorAB.x * vectorBC.x) +
                (vectorAB.y * vectorBC.y) +
                (vectorAB.z * vectorBC.z)

        val magnitudeAB = kotlin.math.sqrt(
            vectorAB.x * vectorAB.x +
                    vectorAB.y * vectorAB.y +
                    vectorAB.z * vectorAB.z
        )

        val magnitudeBC = kotlin.math.sqrt(
            vectorBC.x * vectorBC.x +
                    vectorBC.y * vectorBC.y +
                    vectorBC.z * vectorBC.z
        )

        val cosTheta = dotProduct / (magnitudeAB * magnitudeBC)

        val crossProduct = Vector3(
            vectorAB.y * vectorBC.z - vectorAB.z * vectorBC.y,
            vectorAB.z * vectorBC.x - vectorAB.x * vectorBC.z,
            vectorAB.x * vectorBC.y - vectorAB.y * vectorBC.x
        )

        val crossMagnitude = kotlin.math.sqrt(
            crossProduct.x * crossProduct.x +
                    crossProduct.y * crossProduct.y +
                    crossProduct.z * crossProduct.z
        )

        val angleRadians = kotlin.math.atan2(crossMagnitude, dotProduct)
        return 180 - Math.toDegrees(angleRadians.toDouble())
    }






    private fun addMarker(anchor: Anchor) {
        val anchorNode = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
        }

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.BLUE))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.009f, Vector3.zero(), material)
                anchorNode.renderable = sphere
            }


        anchorNode.setOnTouchListener { hitTestResult, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                val hit = arFragment.arSceneView.arFrame?.hitTest(motionEvent)?.firstOrNull()
                if (hit != null) {
                    moveAnchor(anchorNode, hit)
                }
            }
            true
        }

        anchorNodes.add(anchorNode)
    }

    private fun moveAnchor(anchorNode: AnchorNode, hitResult: HitResult) {
        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { redMaterial ->
                val sphere = ShapeFactory.makeSphere(0.009f, Vector3.zero(), redMaterial)
                anchorNode.renderable = sphere
            }

        anchorNode.anchor?.detach()

        val newAnchor = hitResult.createAnchor()
        anchorNode.anchor = newAnchor

        android.os.Handler(mainLooper).postDelayed({
            MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.BLUE))
                .thenAccept { blueMaterial ->
                    val sphere = ShapeFactory.makeSphere(0.009f, Vector3.zero(), blueMaterial)
                    anchorNode.renderable = sphere
                }
        }, 500)

        updateDistancesAndTexts()
    }

    private fun updateDistancesAndTexts() {

        textNodes.forEach { it.setParent(null) }
        textNodes.clear()

        if (anchorNodes.size > 1) {
            val anchors = anchorNodes.mapNotNull { it.anchor }

            for (i in 0 until anchors.size - 1) {
                val distance = calculateDistance(anchors[i], anchors[i + 1])
                val midPoint = calculateMidPoint(anchors[i], anchors[i + 1])
                showDistanceText(midPoint, distance)
            }
        }
    }
    private fun showAreaText(anchors: List<Anchor>, area: Double) {
        val center = calculatePolygonCenter(anchors)

        ViewRenderable.builder()
            .setView(this, R.layout.area_text_view)
            .build()
            .thenAccept { renderable ->
                val textNode = AnchorNode().apply {
                    worldPosition = center
                }
                textNode.renderable = renderable

                val textView = renderable.view.findViewById<TextView>(R.id.areaTextView)
                textView.text = "${"%.4f".format(area)} м²"

                arFragment.arSceneView.scene.addChild(textNode)
                textNodes.add(textNode)

                arFragment.arSceneView.scene.addOnUpdateListener {
                    val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
                    val directionToCamera = Vector3.subtract(cameraPosition, textNode.worldPosition).normalized()

                    val distanceToCamera = Vector3.subtract(textNode.worldPosition, cameraPosition).length()
                    val scaleFactor = distanceToCamera / 2.0f
                    textNode.localScale = Vector3(scaleFactor, scaleFactor, scaleFactor)

                    textNode.worldRotation = Quaternion.lookRotation(directionToCamera, Vector3.up())
                }
            }
            .exceptionally { throwable ->
                Log.e("AR_AREA", getString(R.string.TextElementError, throwable.message))
                null
            }
    }
    private fun showCornerText(anchors: List<Anchor>, area: Double) {
        val center = calculatePolygonCenter(anchors)

        ViewRenderable.builder()
            .setView(this, R.layout.area_text_view)
            .build()
            .thenAccept { renderable ->
                val textNode = AnchorNode().apply {
                    worldPosition = center
                }
                textNode.renderable = renderable

                val textView = renderable.view.findViewById<TextView>(R.id.areaTextView)
                textView.text = "${"%.2f".format(area)} deg."

                arFragment.arSceneView.scene.addChild(textNode)
                textNodes.add(textNode)

                arFragment.arSceneView.scene.addOnUpdateListener {
                    val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
                    val directionToCamera = Vector3.subtract(cameraPosition, textNode.worldPosition).normalized()

                    val distanceToCamera = Vector3.subtract(textNode.worldPosition, cameraPosition).length()
                    val scaleFactor = distanceToCamera / 2.0f
                    textNode.localScale = Vector3(scaleFactor, scaleFactor, scaleFactor)

                    textNode.worldRotation = Quaternion.lookRotation(directionToCamera, Vector3.up())
                }
            }
            .exceptionally { throwable ->
                Log.e("AR_AREA", getString(R.string.TextElementError, throwable.message))
                null
            }
    }

    private fun calculatePolygonCenter(anchors: List<Anchor>): Vector3 {
        val x = anchors.map { it.pose.tx() }.average().toFloat()
        val y = anchors.map { it.pose.ty() }.average().toFloat()
        val z = anchors.map { it.pose.tz() }.average().toFloat()
        return Vector3(x, y, z)
    }
    private fun showDistanceText(position: Vector3, distance: Double) {
        val unit = sharedPreferences.getString("unit", "meters")

        val (distanceInSelectedUnit, displayUnit) = when (unit) {
            "inches" -> {
                val distanceInInches = distance * 39.3701
                distanceInInches to "дюйм"

            }
            "meters" -> {
                when {
                    distance >= 1.0 -> {
                        distance to "м"
                    }
                    distance >= 0.01 -> {
                        val distanceInCentimeters = distance * 100
                        distanceInCentimeters to "см"
                    }
                    else -> {
                        val distanceInCentimeters = distance * 100
                        distanceInCentimeters to "см"
                    }
                }
            }
            else -> {
                when {
                    distance >= 1.0 -> {
                        distance to "м"
                    }
                    distance >= 0.01 -> {
                        val distanceInCentimeters = distance * 100
                        distanceInCentimeters to "см"
                    }
                    else -> {
                        val distanceInCentimeters = distance * 100
                        distanceInCentimeters to "см"
                    }
                }
            }
        }

        ViewRenderable.builder()
            .setView(this, R.layout.distance_text_view)
            .build()
            .thenAccept { renderable ->
                val textNode = AnchorNode().apply {
                    worldPosition = position
                }
                textNode.renderable = renderable

                val textView = renderable.view.findViewById<TextView>(R.id.distanceTextView)
                textView.text = "${"%.2f".format(distanceInSelectedUnit)} $displayUnit"

                arFragment.arSceneView.scene.addChild(textNode)
                textNodes.add(textNode)

                arFragment.arSceneView.scene.addOnUpdateListener {
                    val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
                    val directionToCamera = Vector3.subtract(cameraPosition, textNode.worldPosition).normalized()

                    val distanceToCamera = Vector3.subtract(textNode.worldPosition, cameraPosition).length()
                    val scaleFactor = distanceToCamera / 2.0f
                    textNode.localScale = Vector3(scaleFactor, scaleFactor, scaleFactor)

                    textNode.worldRotation = Quaternion.lookRotation(directionToCamera, Vector3.up())
                }
            }
            .exceptionally { throwable ->
                Log.e("AR_TEXT", getString(R.string.TextElementError, throwable.message))
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
        if (anchors.isNotEmpty() && anchorNodes.isNotEmpty()) {
            anchors.removeAt(anchors.size - 1).detach()

            anchorNodes.removeAt(anchorNodes.size - 1).setParent(null)


            if (textNodes.isNotEmpty()) {
                textNodes.removeAt(textNodes.size - 1).setParent(null)
            }

            Toast.makeText(this, getString(R.string.LastDotClearToast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.NotDotsToClearToast), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this,
                        getString(R.string.ErrorPhotoCreationToast), Toast.LENGTH_SHORT).show()
                }
            },
            android.os.Handler(mainLooper)
        )
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val projectPath = intent.getStringExtra("projectPath")
        if (projectPath.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.ProjectNotChosenToast), Toast.LENGTH_SHORT).show()
            return
        }

        val projectFolder = File(projectPath)
        if (!projectFolder.exists()) {
            projectFolder.mkdirs()
        }
        val selectedTool = sharedPreferences.getString("selected_tool2", "png")
        if(selectedTool == "png"){
            val filename = "AR_Screenshot_${System.currentTimeMillis()}.png"
            val file = File(projectFolder, filename)
            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(this,
                        getString(R.string.PhotoSavedToast, file.absolutePath), Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.SavedPhotoError), Toast.LENGTH_SHORT).show()
            }
        }else if(selectedTool == "jpg"){
            val filename = "AR_Screenshot_${System.currentTimeMillis()}.jpg"
            val file = File(projectFolder, filename)
            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    Toast.makeText(this,
                        getString(R.string.PhotoSavedToast, file.absolutePath), Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.SavedPhotoError), Toast.LENGTH_SHORT).show()
            }
        }else if(selectedTool == "exif") {
            val filename = "AR_Screenshot_${System.currentTimeMillis()}.jpg"
            val file = File(projectFolder, filename)
            val exifFile = File(projectFolder, "AR_Screenshot_${System.currentTimeMillis()}.exif")

            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                val exif = ExifInterface(file.absolutePath)

                exifFile.writeText("Дата: ${exif.getAttribute(ExifInterface.TAG_DATETIME)}\n")
                exifFile.appendText("Камера: ${exif.getAttribute(ExifInterface.TAG_MAKE)} ${exif.getAttribute(ExifInterface.TAG_MODEL)}\n")
                exifFile.appendText("GPS: ${exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)}, ${exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)}\n")

                Toast.makeText(this, getString(R.string.PhotoSavedToast, file.absolutePath), Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "EXIF сохранен в ${exifFile.absolutePath}", Toast.LENGTH_SHORT).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.SavedPhotoError), Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Ошибка камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val photoFile = File(externalMediaDirs.first(), "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@MainActivity, "Фото сохранено: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    previewView.visibility = View.GONE
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
                    previewView.visibility = View.GONE
                }
            })
    }

}
