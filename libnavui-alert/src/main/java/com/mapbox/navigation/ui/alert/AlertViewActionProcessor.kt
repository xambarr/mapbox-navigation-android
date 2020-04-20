package com.mapbox.navigation.ui.alert

import android.view.View.VISIBLE
import kotlinx.coroutines.flow.flow

class AlertViewActionProcessor {
    fun showViewProcessor(text: String) = flow {
        emit(AlertState(text, VISIBLE))
    }
}