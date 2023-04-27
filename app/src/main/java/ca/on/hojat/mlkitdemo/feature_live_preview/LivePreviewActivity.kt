package ca.on.hojat.mlkitdemo.feature_live_preview

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ca.on.hojat.mlkitdemo.R
import ca.on.hojat.mlkitdemo.shared.VisionImageProcessor
import ca.on.hojat.mlkitdemo.shared.barcodescanner.BarcodeScannerProcessor
import ca.on.hojat.mlkitdemo.shared.facedetector.FaceDetectorProcessor
import ca.on.hojat.mlkitdemo.shared.facemeshdetector.FaceMeshDetectorProcessor
import ca.on.hojat.mlkitdemo.shared.labeldetector.LabelDetectorProcessor
import ca.on.hojat.mlkitdemo.shared.objectdetector.ObjectDetectorProcessor
import ca.on.hojat.mlkitdemo.shared.posedetector.PoseDetectorProcessor
import ca.on.hojat.mlkitdemo.shared.preference.PreferenceUtils
import ca.on.hojat.mlkitdemo.shared.segmenter.SegmenterProcessor
import ca.on.hojat.mlkitdemo.shared.textdetector.TextRecognitionProcessor
import ca.on.hojat.mlkitdemo.databinding.ActivityVisionCameraxLivePreviewBinding
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Live preview demo app for ML Kit APIs using CameraX. */
@KeepName
class LivePreviewActivity : AppCompatActivity(), OnItemSelectedListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityVisionCameraxLivePreviewBinding

    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = OBJECT_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisionCameraxLivePreviewBinding.inflate(layoutInflater)

        Log.d(TAG, "onCreate")
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION)
        }
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        setContentView(binding.root)


        val options: MutableList<String> = ArrayList()
        options.add(OBJECT_DETECTION)
        options.add(OBJECT_DETECTION_CUSTOM)
        options.add(CUSTOM_AUTOML_OBJECT_DETECTION)
        options.add(FACE_DETECTION)
        options.add(BARCODE_SCANNING)
        options.add(IMAGE_LABELING)
        options.add(IMAGE_LABELING_CUSTOM)
        options.add(CUSTOM_AUTOML_LABELING)
        options.add(POSE_DETECTION)
        options.add(SELFIE_SEGMENTATION)
        options.add(TEXT_RECOGNITION_LATIN)
        options.add(TEXT_RECOGNITION_CHINESE)
        options.add(TEXT_RECOGNITION_DEVANAGARI)
        options.add(TEXT_RECOGNITION_JAPANESE)
        options.add(TEXT_RECOGNITION_KOREAN)
        options.add(FACE_MESH_DETECTION)

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        binding.spinner.adapter = dataAdapter
        binding.spinner.onItemSelectedListener = this
        binding.facingSwitch.setOnCheckedChangeListener(this)
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LivePreviewViewModel::class.java].processCameraProvider.observe(
            this
        ) { provider: ProcessCameraProvider? ->
            cameraProvider = provider
            bindAllCameraUseCases()
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_SELECTED_MODEL, selectedModel)
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent?.getItemAtPosition(pos).toString()
        Log.d(TAG, "Selected model: $selectedModel")
        bindAnalysisUseCase()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (cameraProvider == null) {
            return
        }
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to $newLensFacing")
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                bindAllCameraUseCases()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
        Toast.makeText(
            applicationContext,
            "This device does not have lens with facing: $newLensFacing",
            Toast.LENGTH_SHORT
        ).show()
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()

        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(binding.previewView.surfaceProvider)
        cameraProvider!!.bindToLifecycle(
            this, cameraSelector!!, previewUseCase
        )
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor = try {
            when (selectedModel) {
                OBJECT_DETECTION -> {
                    Log.i(TAG, "Using Object Detector Processor")
                    val objectDetectorOptions =
                        PreferenceUtils.getObjectDetectorOptionsForLivePreview(this)
                    ObjectDetectorProcessor(this, objectDetectorOptions)
                }

                OBJECT_DETECTION_CUSTOM -> {
                    Log.i(TAG, "Using Custom Object Detector (with object labeler) Processor")
                    val localModel =
                        LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite")
                            .build()
                    val customObjectDetectorOptions =
                        PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                            this, localModel
                        )
                    ObjectDetectorProcessor(this, customObjectDetectorOptions)
                }

                CUSTOM_AUTOML_OBJECT_DETECTION -> {
                    Log.i(TAG, "Using Custom AutoML Object Detector Processor")
                    val customAutoMLODTLocalModel =
                        LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json")
                            .build()
                    val customAutoMLODTOptions =
                        PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                            this, customAutoMLODTLocalModel
                        )
                    ObjectDetectorProcessor(this, customAutoMLODTOptions)
                }

                TEXT_RECOGNITION_LATIN -> {
                    Log.i(TAG, "Using on-device Text recognition Processor for Latin")
                    TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
                }

                TEXT_RECOGNITION_CHINESE -> {
                    Log.i(
                        TAG, "Using on-device Text recognition Processor for Latin and Chinese"
                    )
                    TextRecognitionProcessor(
                        this, ChineseTextRecognizerOptions.Builder().build()
                    )
                }

                TEXT_RECOGNITION_DEVANAGARI -> {
                    Log.i(
                        TAG, "Using on-device Text recognition Processor for Latin and Devanagari"
                    )
                    TextRecognitionProcessor(
                        this, DevanagariTextRecognizerOptions.Builder().build()
                    )
                }

                TEXT_RECOGNITION_JAPANESE -> {
                    Log.i(
                        TAG, "Using on-device Text recognition Processor for Latin and Japanese"
                    )
                    TextRecognitionProcessor(
                        this, JapaneseTextRecognizerOptions.Builder().build()
                    )
                }

                TEXT_RECOGNITION_KOREAN -> {
                    Log.i(
                        TAG, "Using on-device Text recognition Processor for Latin and Korean"
                    )
                    TextRecognitionProcessor(
                        this, KoreanTextRecognizerOptions.Builder().build()
                    )
                }

                FACE_DETECTION -> {
                    Log.i(TAG, "Using Face Detector Processor")
                    val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
                    FaceDetectorProcessor(this, faceDetectorOptions)
                }

                BARCODE_SCANNING -> {
                    Log.i(TAG, "Using Barcode Detector Processor")
                    BarcodeScannerProcessor(this)
                }

                IMAGE_LABELING -> {
                    Log.i(TAG, "Using Image Label Detector Processor")
                    LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS)
                }

                IMAGE_LABELING_CUSTOM -> {
                    Log.i(TAG, "Using Custom Image Label (Birds) Detector Processor")
                    val localClassifier = LocalModel.Builder()
                        .setAssetFilePath("custom_models/bird_classifier.tflite").build()
                    val customImageLabelerOptions =
                        CustomImageLabelerOptions.Builder(localClassifier).build()
                    LabelDetectorProcessor(this, customImageLabelerOptions)
                }

                CUSTOM_AUTOML_LABELING -> {
                    Log.i(TAG, "Using Custom AutoML Image Label Detector Processor")
                    val customAutoMLLabelLocalModel =
                        LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json")
                            .build()
                    val customAutoMLLabelOptions =
                        CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                            .setConfidenceThreshold(0f).build()
                    LabelDetectorProcessor(this, customAutoMLLabelOptions)
                }

                POSE_DETECTION -> {
                    val poseDetectorOptions =
                        PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
                    val shouldShowInFrameLikelihood =
                        PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
                    val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
                    val rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)
                    val runClassification =
                        PreferenceUtils.shouldPoseDetectionRunClassification(this)
                    PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        runClassification,
                        /* isStreamMode = */
                        true
                    )
                }

                SELFIE_SEGMENTATION -> SegmenterProcessor(this)
                FACE_MESH_DETECTION -> FaceMeshDetectorProcessor(this)
                else -> throw IllegalStateException("Invalid model name")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: $selectedModel", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val builder = ImageAnalysis.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    binding.graphicOverlay.setImageSourceInfo(
                        imageProxy.width, imageProxy.height, isImageFlipped
                    )
                } else {
                    binding.graphicOverlay.setImageSourceInfo(
                        imageProxy.height, imageProxy.width, isImageFlipped
                    )
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }
            try {
                binding.graphicOverlay.let { imageProcessor!!.processImageProxy(imageProxy, it) }
            } catch (e: MlKitException) {
                Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        cameraProvider!!.bindToLifecycle(
            this, cameraSelector!!, analysisUseCase
        )
    }

    companion object {
        private const val TAG = "CameraXLivePreview"
        private const val OBJECT_DETECTION = "Object Detection"
        private const val OBJECT_DETECTION_CUSTOM = "Custom Object Detection"
        private const val CUSTOM_AUTOML_OBJECT_DETECTION = "Custom AutoML Object Detection (Flower)"
        private const val FACE_DETECTION = "Face Detection"
        private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
        private const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese (Beta)"
        private const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari (Beta)"
        private const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese (Beta)"
        private const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean (Beta)"
        private const val BARCODE_SCANNING = "Barcode Scanning"
        private const val IMAGE_LABELING = "Image Labeling"
        private const val IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)"
        private const val CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)"
        private const val POSE_DETECTION = "Pose Detection"
        private const val SELFIE_SEGMENTATION = "Selfie Segmentation"
        private const val FACE_MESH_DETECTION = "Face Mesh Detection (Beta)"

        private const val STATE_SELECTED_MODEL = "selected_model"
    }
}
