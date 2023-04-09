package ca.on.hojat.mlkitdemo.cameralivepreviewvisiondetectors

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class CameraXViewModel(application: Application) : AndroidViewModel(application) {

    // Handle any errors (including cancellation) here.
    val processCameraProvider: LiveData<ProcessCameraProvider>
        get() {
            TODO()
        }


}