package ca.on.hojat.mlkitdemo

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.cameralivepreviewvisiondetectors.CameraXLivePreviewActivity
import ca.on.hojat.mlkitdemo.databinding.ActivityMainBinding
import ca.on.hojat.mlkitdemo.livecameratranslator.LiveCameraTranslatorActivity
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
                            Intent(applicationContext, CameraXLivePreviewActivity::class.java)
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
                    else -> {
                        Toast.makeText(this, optionsList[position], Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}