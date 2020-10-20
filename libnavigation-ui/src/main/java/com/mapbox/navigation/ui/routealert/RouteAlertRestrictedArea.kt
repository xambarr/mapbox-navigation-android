package com.mapbox.navigation.ui.routealert

import android.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.toLineString
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.generateLineLayerProperties

class RouteAlertRestrictedArea(style: Style,
                               restrictedAreaProperties: Array<PropertyValue<out Any>>) {
    constructor(style: Style) : this(
        style = style,
        restrictedAreaProperties = generateLineLayerProperties(
            MAPBOX_RESTRICTED_AREA_LINE_COLOR,
            MAPBOX_RESTRICTED_AREA_LINE_WIDTH
        )
    )

    private val restrictedAreaSource = GeoJsonSource(MAPBOX_RESTRICTED_AREA_SOURCE)
    private val restrictedAreaLayer = LineLayer(
        MAPBOX_RESTRICTED_AREA_LAYER,
        MAPBOX_RESTRICTED_AREA_SOURCE
    ).withProperties(*restrictedAreaProperties)

    private var routeLineString: LineString? = null

    init {
        style.addSource(restrictedAreaSource)
        style.addLayer(restrictedAreaLayer)
    }

    fun setRouteLineString(routeLineString: LineString) {
        this.routeLineString = routeLineString
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<RestrictedAreaAlert>().run {
            val restrictedAreasFeatures = mutableListOf<Feature>()
            forEach {
                val alertGeometry = it.alertGeometry
                val routeLineString = routeLineString
                if (alertGeometry != null && routeLineString != null) {
                    val restrictedAreaLineString =
                        alertGeometry.toLineString(routeLineString)
                    restrictedAreasFeatures.add(
                        Feature.fromGeometry(restrictedAreaLineString)
                    )
                } else {
                    // TODO: log the error
                    return@forEach
                }
            }
            restrictedAreaSource.setGeoJson(
                FeatureCollection.fromFeatures(restrictedAreasFeatures)
            )
        }
    }

    companion object {
        const val MAPBOX_RESTRICTED_AREA_SOURCE = "mapbox_restricted_area_source"
        const val MAPBOX_RESTRICTED_AREA_LAYER = "mapbox_restricted_area_layer"
        private const val MAPBOX_RESTRICTED_AREA_LINE_COLOR = Color.RED
        private const val MAPBOX_RESTRICTED_AREA_LINE_WIDTH = 10f
    }
}
