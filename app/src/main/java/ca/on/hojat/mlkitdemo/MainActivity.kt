package ca.on.hojat.mlkitdemo

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.databinding.ActivityMainBinding


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
                Toast.makeText(this, optionsList[position], Toast.LENGTH_SHORT).show()
            }


    }
}