package com.packt.myapplication


import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val radioGroup = findViewById<RadioGroup>(R.id.tools_radio_group2)
        val selectedTool1 = sharedPreferences.getString("unit", "cm")
        when (selectedTool1) {
            "cm" -> radioGroup.check(R.id.tool_cm)
            "inches" -> radioGroup.check(R.id.tool_inches)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.tool_cm -> {
                    editor.putString("unit", "cm")
                }
                R.id.tool_inches -> {
                    editor.putString("unit", "inches")
                }
            }
            editor.apply()
        }
        val toolsRadioGroup = findViewById<RadioGroup>(R.id.tools_radio_group)


        val selectedTool = sharedPreferences.getString("selected_tool", "measure")
        when (selectedTool) {
            "measure" -> toolsRadioGroup.check(R.id.tool_measure)
            "area" -> toolsRadioGroup.check(R.id.tool_area)
            "corner" -> toolsRadioGroup.check(R.id.tool_corner)
        }


        toolsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.tool_measure -> editor.putString("selected_tool", "measure")
                R.id.tool_area -> editor.putString("selected_tool", "area")
                R.id.tool_corner -> editor.putString("selected_tool", "corner")
            }
            editor.apply()
        }
        val toolsRadioGroup2 = findViewById<RadioGroup>(R.id.tools_radio_file)


        val selectedTool2 = sharedPreferences.getString("selected_tool2", "jpg")
        when (selectedTool2) {
            "png" -> toolsRadioGroup.check(R.id.tool_png)
            "jpg" -> toolsRadioGroup.check(R.id.tool_jpg)
        }


        toolsRadioGroup2.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.tool_jpg -> editor.putString("selected_tool2", "jpg")
                R.id.tool_png -> editor.putString("selected_tool2", "png")
            }
            editor.apply()
        }

    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
