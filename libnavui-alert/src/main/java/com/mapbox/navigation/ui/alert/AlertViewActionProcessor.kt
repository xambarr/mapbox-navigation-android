package com.mapbox.navigation.ui.alert

import android.view.View.VISIBLE
import com.mapbox.navigation.ui.base.model.AlertState
import kotlinx.coroutines.flow.flow

class AlertViewActionProcessor {
    fun showViewProcessor(text: String) = flow {
        emit(AlertState(text, VISIBLE))
    }
}