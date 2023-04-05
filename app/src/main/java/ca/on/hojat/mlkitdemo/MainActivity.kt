package ca.on.hojat.mlkitdemo

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val countryList = arrayListOf(
        "CameraX live preview vision detectors",
        "Still image vision detector",
        "Live camera translator",
        "Code scanner",
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
            countryList
        )
        binding.mainLv.adapter = adapter


    }
}