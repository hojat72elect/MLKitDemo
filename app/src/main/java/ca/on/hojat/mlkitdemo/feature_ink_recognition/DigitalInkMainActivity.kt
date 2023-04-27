package ca.on.hojat.mlkitdemo.feature_ink_recognition

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import ca.on.hojat.mlkitdemo.databinding.ActivityDigitalInkMainKotlinBinding
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSortedSet
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import java.util.Locale

/** Main activity which creates a StrokeManager and connects it to the DrawingView. */
class DigitalInkMainActivity : AppCompatActivity(), StrokeManager.DownloadedModelsChangedListener {

    private lateinit var binding: ActivityDigitalInkMainKotlinBinding

    @JvmField
    @VisibleForTesting
    val strokeManager = StrokeManager()
    private lateinit var languageAdapter: ArrayAdapter<ModelLanguageContainer>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDigitalInkMainKotlinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.drawingView.setStrokeManager(strokeManager)
        binding.statusTextView.setStrokeManager(strokeManager)
        strokeManager.setStatusChangedListener(binding.statusTextView)
        strokeManager.setContentChangedListener(binding.drawingView)
        strokeManager.setDownloadedModelsChangedListener(this)
        strokeManager.setClearCurrentInkAfterRecognition(true)
        strokeManager.setTriggerRecognitionAfterInput(false)
        languageAdapter = populateLanguageAdapter()
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languagesSpinner.adapter = languageAdapter
        strokeManager.refreshDownloadedModelsStatus()

        binding.languagesSpinner.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val languageCode =
                        (parent.adapter.getItem(position) as ModelLanguageContainer).languageTag
                            ?: return
                    Log.i(TAG, "Selected language: $languageCode")
                    strokeManager.setActiveModel(languageCode)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.i(TAG, "No language selected")
                }
            }
        strokeManager.reset()

        // register some other listeners
        binding.downloadButton.setOnClickListener {
            strokeManager.download()
        }
        binding.recognizeButton.setOnClickListener {
            strokeManager.recognize()
        }
        binding.clearButton.setOnClickListener {
            strokeManager.reset()
            binding.drawingView.clear()
        }
        binding.deleteButton.setOnClickListener {
            strokeManager.deleteActiveModel()
        }
    }

    private class ModelLanguageContainer
    private constructor(private val label: String, val languageTag: String?) :
        Comparable<ModelLanguageContainer> {

        var downloaded: Boolean = false

        override fun toString(): String {
            return when {
                languageTag == null -> label
                downloaded -> "   [D] $label"
                else -> "   $label"
            }
        }

        override fun compareTo(other: ModelLanguageContainer): Int {
            return label.compareTo(other.label)
        }

        companion object {
            /** Populates and returns a real model identifier, with label and language tag. */
            fun createModelContainer(label: String, languageTag: String?): ModelLanguageContainer {
                // Offset the actual language labels for better readability
                return ModelLanguageContainer(label, languageTag)
            }

            /** Populates and returns a label only, without a language tag. */
            fun createLabelOnly(label: String): ModelLanguageContainer {
                return ModelLanguageContainer(label, null)
            }
        }
    }

    private fun populateLanguageAdapter(): ArrayAdapter<ModelLanguageContainer> {
        val languageAdapter =
            ArrayAdapter<ModelLanguageContainer>(this, android.R.layout.simple_spinner_item)
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Select language"))
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Non-text Models"))

        // Manually add non-text models first
        for (languageTag in NON_TEXT_MODELS.keys) {
            languageAdapter.add(
                ModelLanguageContainer.createModelContainer(
                    NON_TEXT_MODELS[languageTag]!!,
                    languageTag
                )
            )
        }
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Text Models"))
        val textModels = ImmutableSortedSet.naturalOrder<ModelLanguageContainer>()
        for (modelIdentifier in DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (NON_TEXT_MODELS.containsKey(modelIdentifier.languageTag)) {
                continue
            }
            if (modelIdentifier.languageTag.endsWith(GESTURE_EXTENSION)) {
                continue
            }
            textModels.add(buildModelContainer(modelIdentifier, "Script"))
        }
        languageAdapter.addAll(textModels.build())
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Gesture Models"))
        val gestureModels = ImmutableSortedSet.naturalOrder<ModelLanguageContainer>()
        for (modelIdentifier in DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (!modelIdentifier.languageTag.endsWith(GESTURE_EXTENSION)) {
                continue
            }
            gestureModels.add(buildModelContainer(modelIdentifier, "Script gesture classifier"))
        }
        languageAdapter.addAll(gestureModels.build())
        return languageAdapter
    }

    private fun buildModelContainer(
        modelIdentifier: DigitalInkRecognitionModelIdentifier,
        labelSuffix: String
    ): ModelLanguageContainer {
        val label = StringBuilder()
        label.append(Locale(modelIdentifier.languageSubtag).displayName)
        if (modelIdentifier.regionSubtag != null) {
            label.append(" (").append(modelIdentifier.regionSubtag).append(")")
        }
        if (modelIdentifier.scriptSubtag != null) {
            label.append(", ").append(modelIdentifier.scriptSubtag).append(" ").append(labelSuffix)
        }
        return ModelLanguageContainer.createModelContainer(
            label.toString(),
            modelIdentifier.languageTag
        )
    }

    override fun onDownloadedModelsChanged(downloadedLanguageTags: Set<String>) {
        for (i in 0 until languageAdapter.count) {
            val container = languageAdapter.getItem(i)!!
            container.downloaded = downloadedLanguageTags.contains(container.languageTag)
        }
        languageAdapter.notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "MLKDI.Activity"
        private val NON_TEXT_MODELS =
            ImmutableMap.of(
                "zxx-Zsym-x-autodraw",
                "Autodraw",
                "zxx-Zsye-x-emoji",
                "Emoji",
                "zxx-Zsym-x-shapes",
                "Shapes"
            )
        private const val GESTURE_EXTENSION = "-x-gesture"
    }
}
