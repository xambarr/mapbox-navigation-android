package com.mapbox.navigation.ui.alert

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.utils.extensions.getStyledAttributes
import kotlinx.android.synthetic.main.layout_alert_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alert View used to report
 * @constructor
 */
class AlertView(
    context: Context?,
    attrSet: AttributeSet?,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrSet, defStyleAttr), AlertViewContract {

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val attributeSet = attrSet

    init {
        inflate(context, R.layout.layout_alert_view, this)

        uiScope.launch {
            render(AlertViewModel.channel.receive())
        }
    }

    override fun render(state: AlertState) {
        context.getStyledAttributes(attributeSet, R.styleable.MapboxNavAlertView) {
            alertTextView.setTextColor(
                ContextCompat.getColor(
                    context, getResourceId(R.styleable.MapboxNavAlertView_avTextColor, Color.BLUE)
                )
            )
        }
        alertTextView.text = state.avText
        visibility = state.viewVisibility
    }
}