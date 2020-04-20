package com.mapbox.navigation.ui.alert

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.base.AlertViewContract
import com.mapbox.navigation.ui.base.model.AlertState
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
class AlertViewX: FrameLayout, AlertViewContract {

    //private val uiScope = CoroutineScope(Dispatchers.Main)
    //private val attributeSet = attrSet

    @JvmOverloads
    constructor(context: Context,
                attrSet: AttributeSet?,
                defStyleAttr: Int = 0): super(context, attrSet, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context,
                attrSet: AttributeSet?,
                defStyleAttr: Int = 0,
                defStyleRes: Int): super(context, attrSet, defStyleAttr, defStyleRes)

    init {
        //inflate(context, R.layout.layout_alert_view, this)
        LayoutInflater.from(context).inflate(R.layout.layout_alert_view, this, true)

//        uiScope.launch {
//            render(AlertViewModel.channel.receive())
//        }
    }

    override fun render(data: AlertState) {
//        context.getStyledAttributes(attributeSet, R.styleable.MapboxNavAlertView) {
//            alertTextView.setTextColor(
//                ContextCompat.getColor(
//                    context, getResourceId(R.styleable.MapboxNavAlertView_avTextColor, Color.BLUE)
//                )
//            )
//        }
        alertTextView.text = data.avText
        visibility = data.viewVisibility
    }
}