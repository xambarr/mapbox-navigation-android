package com.mapbox.navigation.ui.routealert

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.ui.R

class RouteAlertRestStop(
    style: Style,
    restStopDrawable: Drawable,
    restStopProperties: Array<PropertyValue<out Any>>) {
    constructor(
        style: Style,
        context: Context
    ) : this(
        style = style,
        restStopDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.mapbox_ic_route_alert_rest_stop
        )!!,
        MapboxRouteAlert.generateSymbolLayerProperties()
    )

    private val restStopSource = GeoJsonSource(MAPBOX_REST_STOP_SOURCE)
    private val restStopLayer = SymbolLayer(
        MAPBOX_REST_STOP_LAYER,
        MAPBOX_REST_STOP_SOURCE
    ).withProperties(
        *restStopProperties,
        PropertyFactory.iconImage(MAPBOX_REST_STOP_IMAGE_PROPERTY_ID),
        PropertyFactory.textField(
            Expression.get(Expression.literal(MAPBOX_REST_STOP_TEXT_PROPERTY_ID))
        ))

    init {
        style.addImage(MAPBOX_REST_STOP_IMAGE_PROPERTY_ID, restStopDrawable)
        style.addSource(restStopSource)
        style.addLayer(restStopLayer)
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<RestStopAlert>().run {
            val restStopFeatures = mutableListOf<Feature>()
            forEach { restStopAlert ->
                val typeString = when (restStopAlert.restStopType) {
                    RestStopType.RestArea -> {
                        "rest area"
                    }
                    RestStopType.Unknown -> {
                        "unknown"
                    }
                    else -> {
                        // TODO: log for error rest stop type
                        return@forEach
                    }
                }
                val feature = Feature.fromGeometry(restStopAlert.coordinate)
                feature.addStringProperty(
                    MAPBOX_REST_STOP_TEXT_PROPERTY_ID,
                    typeString
                )
                restStopFeatures.add(feature)
            }
            restStopSource.setGeoJson(
                FeatureCollection.fromFeatures(restStopFeatures)
            )
        }
    }

    companion object {
        const val MAPBOX_REST_STOP_SOURCE = "mapbox_rest_stop_source"
        const val MAPBOX_REST_STOP_LAYER = "mapbox_rest_stop_layer"
        const val MAPBOX_REST_STOP_TEXT_PROPERTY_ID = "mapbox_rest_stop_text_property_id"
        const val MAPBOX_REST_STOP_IMAGE_PROPERTY_ID = "mapbox_rest_stop_image_property_id"
    }
}
