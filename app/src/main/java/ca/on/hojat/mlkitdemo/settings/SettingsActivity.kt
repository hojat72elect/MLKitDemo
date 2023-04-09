package ca.on.hojat.mlkitdemo.settings

import android.preference.PreferenceFragment
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.R

class SettingsActivity : AppCompatActivity() {


    /**
     * Specifies where this activity is launched from.
     */
    enum class LaunchSource(
        val titleResId: Int,
        val prefFragmentClass: Class<out PreferenceFragment?>
    ) {
        CAMERAX_LIVE_PREVIEW(
            R.string.pref_screen_title_camerax_live_preview,
            CameraXLivePreviewPreferenceFragment::class.java
        ),
        STILL_IMAGE(
            R.string.pref_screen_title_still_image,
            StillImagePreferenceFragment::class.java
        ),
        LIVE_CAMERA_TRANSLATOR(
            R.string.pref_screen_title_live_camera_translator,
            LiveCameraTranslatorPreferenceFragment::class.java
        ),
        DIGITAL_INK_RECOGNITION(
            R.string.pref_screen_title_digital_ink_recognition,
            DigitalInkRecognitionPreferenceFragment::class.java
        )
    }


    companion object {
        const val EXTRA_LAUNCH_SOURCE = "extra_launch_source"
    }
}
