package com.mapbox.navigation.ui.base

import com.mapbox.navigation.ui.base.model.AlertState

interface AlertViewContract {
    fun render(data: AlertState)
}

