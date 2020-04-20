package com.mapbox.navigation.ui.core

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.alert.AlertViewX
import com.mapbox.navigation.ui.base.AlertViewContract
import com.mapbox.navigation.ui.base.model.AlertState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

typealias ActivityStateCmd<T> = (activity: T) -> Unit
typealias ViewEffect<T1,T2> = (input: T1) -> ActivityStateCmd<T2>

val resumeExampleViewUpdateCmd: ViewEffect<AlertState, Activity> = { input -> { activity ->
    activity.window.findViewById<View>(R.id.mapboxAlertView)?.let {
        if(it is AlertViewContract) {
            it.render(input)
        }
    }
}  }

class MapboxUICoreViewModel: ViewModel() {

    private val viewCommands = Channel<ActivityStateCmd<Activity>>(Channel.UNLIMITED)

    fun getViewCommandUpdates(): ReceiveChannel<ActivityStateCmd<Activity>> = viewCommands

    fun resumeExample(input: String) {
//        val result = someExampleInteractor(input)
//        val cmd = resumeExampleViewUpdateCmd(result)
//        listOf(cmd).emit()
    }

    fun handleRouteProgress(routeProgress: Int) {
        // call some interactor with route progress and emit an alert state
        val alertState = AlertState("On Route Progress: $routeProgress", View.VISIBLE)
        val cmd = resumeExampleViewUpdateCmd(alertState)
        listOf(cmd).emit()
    }

    private fun List<ActivityStateCmd<Activity>>.emit() {
        this.forEach { cmd ->
            if(!viewCommands.isClosedForSend) {
                viewCommands.offer(cmd)
            }
        }
    }
}