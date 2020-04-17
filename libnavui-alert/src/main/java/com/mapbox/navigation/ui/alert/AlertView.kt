package com.mapbox.navigation.ui.alert

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class AlertView(
    context: Context?,
    attributeSet: AttributeSet?,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attributeSet, defStyleAttr), AlertViewContract {

    init {
        inflate(context, R.layout.layout_alert_view, this)
    }

    override fun render(state : AlertState) {

    }
}