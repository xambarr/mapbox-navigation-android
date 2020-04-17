package com.mapbox.navigation.ui.alert

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AlertViewModel(app: Application) : AndroidViewModel(app) {
    private var currentState: AlertState? = null

    fun onRouteProgressChanged() {
        // val currentState = create a new object AlertState
        // somehow call the view.render(currentState)
    }

    fun showAlertView() {
        // val currentState = create a new object AlertState
        // somehow call the view.render(currentState)
        // alertViewCallback.onViewVisible()
    }

    fun hideAlertView() {
        // val currentState = create a new object AlertState
        // somehow call the view.render(currentState)
        // alertViewCallback.onViewGone()
    }
}