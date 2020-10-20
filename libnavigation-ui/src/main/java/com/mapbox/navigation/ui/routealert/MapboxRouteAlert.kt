package com.mapbox.navigation.ui.routealert

import android.content.Context
import androidx.annotation.ColorInt
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.navigation.base.trip.model.alert.RouteAlert

class MapboxRouteAlert(context: Context, style: Style) {

    private val routeAlertToll = RouteAlertToll(style, context)
    private val routeAlertTunnel = RouteAlertTunnel(style, context)
    private val routeAlertCountryBorderCrossing = RouteAlertCountryBorderCrossing(style, context)
    private val routeAlertRestStop = RouteAlertRestStop(style, context)
    private val routeAlertRestrictedArea = RouteAlertRestrictedArea(style)

    fun setRouteLineString(routeLineString: LineString) {
        routeAlertTunnel.setRouteLineString(routeLineString)
        routeAlertRestrictedArea.setRouteLineString(routeLineString)
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlertToll.onNewRouteAlerts(routeAlerts)
        routeAlertTunnel.onNewRouteAlerts(routeAlerts)
        routeAlertCountryBorderCrossing.onNewRouteAlerts(routeAlerts)
        routeAlertRestStop.onNewRouteAlerts(routeAlerts)
        routeAlertRestrictedArea.onNewRouteAlerts(routeAlerts)
    }

    companion object {
        fun generateLineLayerProperties(
            @ColorInt color: Int,
            width: Float
        ): Array<PropertyValue<out Any>> =
            arrayOf(
                PropertyFactory.lineColor(color),
                PropertyFactory.lineWidth(width)
            )

        fun getMapboxTunnelNameLayerProperties(
        ): Array<PropertyValue<out Any>> = arrayOf(
            PropertyFactory.iconSize(3f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM_RIGHT),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true)
        )

        fun generateSymbolLayerProperties(): Array<PropertyValue<out Any>> = arrayOf(
            PropertyFactory.iconSize(3f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
    }
}
