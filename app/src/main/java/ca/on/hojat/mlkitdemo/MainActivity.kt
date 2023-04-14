package ca.on.hojat.mlkitdemo

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.live_preview_cv.LivePreviewActivity
import ca.on.hojat.mlkitdemo.databinding.ActivityMainBinding
import ca.on.hojat.mlkitdemo.digital_ink_recognition.DigitalInkMainActivity
import ca.on.hojat.mlkitdemo.camera_translator.LiveCameraTranslatorActivity
import ca.on.hojat.mlkitdemo.stillimagevisiondetector.StillImageActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val optionsList = arrayListOf(
        "CameraX live preview vision detectors",
        "Still image vision detector",
        "Live camera translator",
        "Digital ink recognition"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // draw the UI
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(
            this,
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            optionsList
        )
        binding.mainLv.adapter = adapter

        // register listeners
        binding.mainLv.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        // go to CameraXLivePreviewActivity
                        val intent =
                            Intent(applicationContext, LivePreviewActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        // go to StillImageActivity
                        val intent = Intent(applicationContext, StillImageActivity::class.java)
                        startActivity(intent)
                    }
                    2 -> {
                        // live camera translator
                        val intent = Intent(
                            applicationContext,
                            LiveCameraTranslatorActivity::class.java
                        )
                        startActivity(intent)
                    }
                    3 -> {
                        val intent = Intent(applicationContext, DigitalInkMainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
    }
}