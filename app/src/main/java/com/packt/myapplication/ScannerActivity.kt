package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.graphics.Color as androidColor
import android.os.Bundle
import android.os.Environment
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
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScannerActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var arFragment: ArFragment
    private lateinit var resetButton: Button
    private lateinit var clearLastButton: Button
    private lateinit var captureButton: Button
    private lateinit var uploadButton: Button
    private val anchors = mutableListOf<Anchor>()
    private val anchorNodes = mutableListOf<AnchorNode>()
    private val textNodes = mutableListOf<AnchorNode>()


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        resetButton = findViewById(R.id.resetButton)
        clearLastButton = findViewById(R.id.clearLastButton)
        captureButton = findViewById(R.id.captureButton)
        uploadButton = findViewById(R.id.uploadButton)
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
        uploadButton.setOnClickListener{
            uploadPhotos()
        }

        clearLastButton.setOnClickListener {
            clearLastAnchor()
        }
        captureButton.setOnClickListener {
            captureScreenshot()
        }
        if (!isOnboardingShown()) {
            showOnboarding(resetButton, captureButton, clearLastButton)
        }
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
        if (anchors.size > 1) {
            val lastIndex = anchors.size - 1
            val distance = calculateDistance(anchors[lastIndex - 1], anchors[lastIndex])
            val midPoint = calculateMidPoint(anchors[lastIndex - 1], anchors[lastIndex])
            Toast.makeText(this, "Дистанция: ${"%.2f".format(distance)} м", Toast.LENGTH_SHORT).show()
            showDistanceText(midPoint, distance)
        }
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
    }
    private fun uploadPhotos() {
        val projectPath = intent.getStringExtra("projectPath")
        if (projectPath.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.ProjectNotChosenToast), Toast.LENGTH_SHORT).show()
            return
        }

        val projectFolder = File(projectPath)
        if (!projectFolder.exists() || !projectFolder.isDirectory) {
            Toast.makeText(this, getString(R.string.InvalidProjectPathToast), Toast.LENGTH_SHORT).show()
            return
        }

        val photoFiles = projectFolder.listFiles { file -> file.name.endsWith(".jpg") }
        if (photoFiles.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.NoPhotosToUploadToast), Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        photoFiles.forEach { photo ->
            Log.d("UploadPhotos", "Uploading file: ${photo.absolutePath}")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    photo.name,
                    photo.asRequestBody("image/jpg".toMediaTypeOrNull())
                )
                .build()

            val uploadRequest = Request.Builder()
                .url("http://194.226.169.23:5000/upload")
                .post(requestBody)
                .build()

            client.newCall(uploadRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UploadPhotos", "Error uploading file: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@ScannerActivity,
                            getString(R.string.UploadFailedToast, photo.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("UploadPhotos", "Response: ${response.code}, body: $responseBody")
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ScannerActivity,
                                getString(R.string.UploadSuccessToast, photo.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        if (photo == photoFiles.last()) {
                            create3DModel(client, projectPath)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@ScannerActivity,
                                getString(R.string.UploadFailedToast, photo.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }

    private fun create3DModel(client: OkHttpClient, projectPath: String) {
        val createObjRequest = Request.Builder()
            .url("http://194.226.169.23:5000/createobj")
            .get()
            .build()

        client.newCall(createObjRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CreateObj", "Error creating 3D model: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@ScannerActivity,
                        getString(R.string.CreateObjFailedToast),
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@ScannerActivity, ScannedProjectActivity::class.java)
                    intent.putExtra("projectPath", projectPath)
                    startActivity(intent)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CreateObj", "Response: ${response.code}, body: $responseBody")
                if (response.isSuccessful) {
                    runOnUiThread {
                        val intent = Intent(this@ScannerActivity, ScannedProjectActivity::class.java)
                        intent.putExtra("projectPath", projectPath)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ScannerActivity,
                            getString(R.string.CreateObjFailedToast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
