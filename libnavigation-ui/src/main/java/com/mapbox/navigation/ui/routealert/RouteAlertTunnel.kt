package com.mapbox.navigation.ui.routealert

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.base.trip.model.alert.toLineString
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.generateLineLayerProperties
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.getMapboxTunnelNameLayerProperties

class RouteAlertTunnel(style: Style,
                       tunnelDrawable: Drawable,
                       tunnelLayerProperties: Array<PropertyValue<out Any>>,
                       tunnelNameLayerProperties: Array<PropertyValue<out Any>>) {
    constructor(style: Style,
                context: Context) : this(style = style,
        tunnelDrawable = ContextCompat.getDrawable(context, R.drawable.mapbox_ic_route_alert_tunnel)!!,
        generateLineLayerProperties(MAPBOX_TUNNEL_LINE_COLOR, MAPBOX_TUNNEL_LINE_WIDTH),
        getMapboxTunnelNameLayerProperties())

    private var routeLineString: LineString? = null

    private val tunnelSource = GeoJsonSource(MAPBOX_TUNNEL_SOURCE)
    private val tunnelLayer = LineLayer(MAPBOX_TUNNEL_LAYER, MAPBOX_TUNNEL_SOURCE)
        .withProperties(*tunnelLayerProperties)

    private val tunnelNameSource = GeoJsonSource(MAPBOX_TUNNEL_NAME_SOURCE)
    private val tunnelNameLayer = SymbolLayer(
        MAPBOX_TUNNEL_NAME_LAYER,
        MAPBOX_TUNNEL_NAME_SOURCE
    ).withProperties(
        *tunnelNameLayerProperties,
        PropertyFactory.iconImage(MAPBOX_TUNNEL_NAME_IMAGE_PROPERTY_ID),
        PropertyFactory.textField(
            Expression.get(Expression.literal(MAPBOX_TUNNEL_NAME_TEXT_PROPERTY_ID))
        ),
    )

    init {
        style.addSource(tunnelSource)
        style.addLayer(tunnelLayer)

        style.addImage(MAPBOX_TUNNEL_NAME_IMAGE_PROPERTY_ID, tunnelDrawable)
        style.addSource(tunnelNameSource)
        style.addLayer(tunnelNameLayer)
    }

    fun setRouteLineString(routeLineString: LineString) {
        this.routeLineString = routeLineString
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<TunnelEntranceAlert>().run {
            val tunnelFeatures = mutableListOf<Feature>()
            val tunnelNamesFeatures = mutableListOf<Feature>()
            forEach { routeAlert ->
                val alertGeometry = routeAlert.alertGeometry
                val routeLineString = routeLineString
                if (alertGeometry != null && routeLineString != null) {
                    val tunnelLineString =
                        alertGeometry.toLineString(routeLineString)
                    tunnelFeatures.add(Feature.fromGeometry(tunnelLineString))
                    routeAlert.info?.name.let {
                        val feature = Feature.fromGeometry(routeAlert.coordinate)
                        feature.addStringProperty(
                            MAPBOX_TUNNEL_NAME_TEXT_PROPERTY_ID,
                            it
                        )
                        tunnelNamesFeatures.add(feature)
                    }
                } else {
                    return@forEach
                }
            }
            tunnelSource.setGeoJson(FeatureCollection.fromFeatures(tunnelFeatures))
            tunnelNameSource.setGeoJson(FeatureCollection.fromFeatures(tunnelNamesFeatures))
        }
    }

    companion object {
        const val MAPBOX_TUNNEL_SOURCE = "mapbox_tunnel_source"
        const val MAPBOX_TUNNEL_LAYER = "mapbox_tunnel_layer"
        private const val MAPBOX_TUNNEL_LINE_COLOR = Color.DKGRAY
        private const val MAPBOX_TUNNEL_LINE_WIDTH = 10f

        const val MAPBOX_TUNNEL_NAME_SOURCE = "mapbox_tunnel_name_source"
        const val MAPBOX_TUNNEL_NAME_LAYER = "mapbox_tunnel_name_layer"
        const val MAPBOX_TUNNEL_NAME_TEXT_PROPERTY_ID = "mapbox_tunnel_name_text_property_id"
        const val MAPBOX_TUNNEL_NAME_IMAGE_PROPERTY_ID = "mapbox_tunnel_name_image_property_id"
    }
}
