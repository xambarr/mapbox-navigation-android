package com.mapbox.navigation.ui.alert

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

class MapboxAlertView(val mapboxNavigation: MapboxNavigation) {

    val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {

        }
    }

    fun initialize() {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    fun destroy() {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }

    fun showAlertView() {

    }

    fun hideView() {

    }
}