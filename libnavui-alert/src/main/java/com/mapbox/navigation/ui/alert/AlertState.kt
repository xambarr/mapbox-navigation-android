package com.mapbox.navigation.ui.alert

import android.view.View

/**
 * Immutable object which contains all the required information to render [AlertView]
 * @property avText String text to show in the alert view
 * @property viewVisibility Int maintains the visibility of the view
 */
data class AlertState(val avText: String, val viewVisibility: Int) {
    companion object {
        /**
         * Returns the initial state of the [AlertView]
         * @return AlertState
         */
        fun idle(): AlertState = AlertState("", View.GONE)
    }
}