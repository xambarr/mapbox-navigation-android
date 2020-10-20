package com.mapbox.navigation.ui.routealert

import androidx.annotation.ColorInt
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_CENTER
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType

/**
 * Default implementation to show different route alerts, refer to [RouteAlertType]
 *
 * @param options for building/displaying the route alert views
 */
class MapboxRouteAlertsDisplayer constructor(
    private var options: MapboxRouteAlertsDisplayOptions
) {
    private val routeAlertToll = RouteAlertToll(
        RouteAlertViewOptions.Builder(options.context, options.style).build()
    )

    /**
     * When [Style] changes, re-add the route alerts to the new style.
     *
     * @param style the latest [Style]
     */
    fun onStyleLoaded(style: Style) {
        options = MapboxRouteAlertsDisplayOptions.Builder(
            options.context,
            style
        ).showToll(options.showToll).build()
        routeAlertToll.onStyleLoaded(style)
    }

    /**
     * Display supported [RouteAlert] on the map.
     * Which types of [RouteAlert] are supported relies on the [options] value. Only supported
     * [RouteAlert] can be handle and displayed on the map.
     *
     * @param routeAlerts a list of route alerts
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        if (options.showToll) {
            routeAlertToll.onNewRouteAlerts(routeAlerts)
        }
    }

    companion object {
        /**
         * Mapbox pre-defined properties line type for route alert.
         */
        fun getMapboxRouteAlertLineLayerProperties(
            @ColorInt color: Int,
            width: Float
        ): Array<PropertyValue<out Any>> =
            arrayOf(
                PropertyFactory.lineColor(color),
                PropertyFactory.lineWidth(width)
            )

        /**
         * Mapbox pre-defined symbol layer properties for route alert.
         * @see [RouteAlertToll]
         */
        fun getMapboxRouteAlertSymbolLayerProperties(): Array<PropertyValue<out Any>> = arrayOf(
            PropertyFactory.iconSize(1.5f),
            PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )

        /**
         * Mapbox pre-defined symbol layer properties for Tunnel name
         */
        fun getMapboxTunnelNameLayerProperties(): Array<PropertyValue<out Any>> = arrayOf(
            PropertyFactory.iconSize(2f),
            PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM_RIGHT),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true)
        )
    }
}
