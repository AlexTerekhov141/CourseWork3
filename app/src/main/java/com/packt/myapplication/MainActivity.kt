package com.packt.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as androidColor
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
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
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

        captureButton.setOnClickListener {
            captureScreenshot()
        }
        if (isOnboardingShown()) {
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
            Toast.makeText(this, "Size: ${"%.2f".format(distance)} м", Toast.LENGTH_SHORT).show()
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
    }

}
