package com.packt.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.ux.ArFragment
import com.packt.myapplication.R
import kotlin.math.sqrt
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class ARActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment
    lateinit var transferButton: Button

    var recordedDistance: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        transferButton = findViewById(R.id.distance)

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _ ->
            if (plane.type == Plane.Type.VERTICAL || plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                recordDistance(hitResult)
            } else {
                Toast.makeText(this, "Выберите вертикальную или горизонтальную поверхность", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialTapTargetPrompt.Builder(this)
            .setTarget(transferButton)
            .setPrimaryText("Измерьте расстояние")
            .setSecondaryText("Сначала измерьте расстояние, затем перейдите в OpenCV")
            .setPromptStateChangeListener { prompt, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED ||
                    state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                    prompt.dismiss()
                }
            }
            .show()

        transferButton.setOnClickListener {
            if (recordedDistance > 0f) {
                val intent = Intent(this, Opencv2Activity::class.java)
                intent.putExtra("distance", recordedDistance)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Сначала измерьте расстояние", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recordDistance(hitResult: HitResult) {
        val frame = arFragment.arSceneView.arFrame
        val cameraPose = frame?.camera?.pose
        val hitPose = hitResult.hitPose
        if (cameraPose != null) {
            val dx = cameraPose.tx() - hitPose.tx()
            val dy = cameraPose.ty() - hitPose.ty()
            val dz = cameraPose.tz() - hitPose.tz()
            recordedDistance = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
            Toast.makeText(this, "Дистанция: ${"%.2f".format(recordedDistance)} м", Toast.LENGTH_SHORT).show()
        }
    }
}
