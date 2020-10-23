package com.mapbox.navigation.ui.routealert

import android.content.Context
import androidx.annotation.ColorInt
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.navigation.base.trip.model.alert.RouteAlert

class MapboxRouteAlert(context: Context, style: Style, private val options: Int = Int.MAX_VALUE) {

    private val routeAlertCountryBorderCrossing = RouteAlertCountryBorderCrossing(style, context)
    private val routeAlertRestrictedArea = RouteAlertRestrictedArea(style)
    private val routeAlertRestStop = RouteAlertRestStop(style, context)
    private val routeAlertToll = RouteAlertToll(style, context)
    private val routeAlertTunnel = RouteAlertTunnel(style, context)

    fun setRouteLineString(routeLineString: LineString) {
        routeAlertTunnel.setRouteLineString(routeLineString)
        routeAlertRestrictedArea.setRouteLineString(routeLineString)
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        if (options and RouteAlertOption.ROUTE_ALERT_COUNTRY_BORDER_CROSSING.option
            == RouteAlertOption.ROUTE_ALERT_COUNTRY_BORDER_CROSSING.option) {
            routeAlertCountryBorderCrossing.onNewRouteAlerts(routeAlerts)
        }

        if (options and RouteAlertOption.ROUTE_ALERT_RESTRICTED_AREA.option
            == RouteAlertOption.ROUTE_ALERT_RESTRICTED_AREA.option) {
            routeAlertRestrictedArea.onNewRouteAlerts(routeAlerts)
        }

        if (options and RouteAlertOption.ROUTE_ALERT_REST_STOP.option
            == RouteAlertOption.ROUTE_ALERT_REST_STOP.option) {
            routeAlertRestStop.onNewRouteAlerts(routeAlerts)
        }

        if (options and RouteAlertOption.ROUTE_ALERT_TOLL.option
            == RouteAlertOption.ROUTE_ALERT_TOLL.option) {
            routeAlertToll.onNewRouteAlerts(routeAlerts)
        }

        if (options and RouteAlertOption.ROUTE_ALERT_TUNNEL.option
            == RouteAlertOption.ROUTE_ALERT_TUNNEL.option) {
            routeAlertTunnel.onNewRouteAlerts(routeAlerts)
        }
    }

    companion object {
        fun getMapboxRouteAlertLineLayerProperties(
            @ColorInt color: Int,
            width: Float
        ): Array<PropertyValue<out Any>> =
            arrayOf(
                PropertyFactory.lineColor(color),
                PropertyFactory.lineWidth(width)
            )

        fun getMapboxRouteAlertSymbolLayerProperties(): Array<PropertyValue<out Any>> = arrayOf(
            PropertyFactory.iconSize(3f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
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
    }
}

enum class RouteAlertOption(val option: Int) {
    ROUTE_ALERT_COUNTRY_BORDER_CROSSING(0x1),
    ROUTE_ALERT_RESTRICTED_AREA(0x10),
    ROUTE_ALERT_REST_STOP(0x100),
    ROUTE_ALERT_TOLL(0X1000),
    ROUTE_ALERT_TUNNEL(0X10000)
}
