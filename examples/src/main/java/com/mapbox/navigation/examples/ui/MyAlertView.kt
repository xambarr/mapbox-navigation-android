package com.mapbox.navigation.examples.ui

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.ui.base.AlertViewContract
import com.mapbox.navigation.ui.base.model.AlertState
import kotlinx.android.synthetic.main.my_alert_layout.view.*

class MyAlertView: FrameLayout, AlertViewContract {

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
        LayoutInflater.from(context).inflate(R.layout.my_alert_layout, this, true)

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
        alertTextView.text = data.avText.toUpperCase()
        visibility = data.viewVisibility
    }
}