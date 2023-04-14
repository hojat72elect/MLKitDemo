package ca.on.hojat.mlkitdemo.camera_translator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.R

class LiveCameraTranslatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_translateshowcase_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LiveCameraTranslatorFragment.newInstance())
                .commitNow()
        }
    }

}
