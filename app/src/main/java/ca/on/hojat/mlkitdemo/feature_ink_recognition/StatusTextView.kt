package ca.on.hojat.mlkitdemo.feature_ink_recognition

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import ca.on.hojat.mlkitdemo.feature_ink_recognition.StrokeManager.StatusChangedListener

/**
 * Status bar for the test app.
 *
 *
 * It is updated upon status changes announced by the StrokeManager.
 */
class StatusTextView : AppCompatTextView, StatusChangedListener {
    private var strokeManager: StrokeManager? = null

    constructor(context: Context) : super(context)
    constructor(context: Context?, attributeSet: AttributeSet?) : super(
        context!!,
        attributeSet
    )

    override fun onStatusChanged() {
        this.text = strokeManager!!.status
    }

    fun setStrokeManager(strokeManager: StrokeManager?) {
        this.strokeManager = strokeManager
    }
}
