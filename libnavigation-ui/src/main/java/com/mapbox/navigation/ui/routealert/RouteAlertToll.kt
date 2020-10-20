package com.mapbox.navigation.ui.routealert

import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.routealert.MapboxRouteAlertsDisplayer.Companion.getMapboxRouteAlertSymbolLayerProperties

/**
 * Handle [TollCollectionAlert] to add an icon on the map to indicate where a toll is presented.
 *
 * @param routeAlertViewOptions the options to build a route alert toll view
 */

class RouteAlertToll(
    private var routeAlertViewOptions: RouteAlertViewOptions
) {
    private lateinit var tollCollectionsSource: GeoJsonSource
    private lateinit var tollCollectionsLayer: SymbolLayer
    private var tollAlertModels: List<RouteAlertModelToll> = emptyList()

    init {
        initialize()
    }

    /**
     * When [Style] changes, re-add the toll route alerts to the new style.
     *
     * @param newStyle the latest [Style]
     */
    fun onStyleLoaded(newStyle: Style) {
        routeAlertViewOptions = routeAlertViewOptions.let { originalOption ->
            RouteAlertViewOptions.Builder(originalOption.context, newStyle).apply {
                originalOption.drawable?.let {
                    drawable(it)
                }
                properties(originalOption.properties)
            }.build()
        }
        initialize()
        onNewRouteTollAlerts(tollAlertModels)
    }

    /**
     * Display toll type route alerts on the map.
     * The [RouteAlertModelToll.tollDescription] is the text shown under the icon.
     *
     * @param tollAlertModels a list of toll alert models
     */
    fun onNewRouteTollAlerts(tollAlertModels: List<RouteAlertModelToll>) {
        this.tollAlertModels = tollAlertModels
        val tollCollectionFeatures = mutableListOf<Feature>()
        tollAlertModels.forEach {
            val feature = Feature.fromGeometry(it.coordinate)
            if (it.tollDescription.isNotEmpty()) {
                feature.addStringProperty(
                    MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID,
                    it.tollDescription
                )
            }
            tollCollectionFeatures.add(feature)
        }
        tollCollectionsSource.setGeoJson(
            FeatureCollection.fromFeatures(tollCollectionFeatures)
        )
    }

    /**
     * Display [TollCollectionAlert] on the map with the default toll description provided by Mapbox.
     *
     * @param routeAlerts a list of route alerts, it may or may not contain [TollCollectionAlert]
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        onNewRouteTollAlerts(
            routeAlerts.filterIsInstance<TollCollectionAlert>().map {
                RouteAlertModelToll(it.coordinate, "")
            }
        )
    }

    private fun initialize() {
        tollCollectionsSource = GeoJsonSource(MAPBOX_TOLL_COLLECTIONS_SOURCE)
        tollCollectionsLayer = SymbolLayer(
            MAPBOX_TOLL_COLLECTIONS_LAYER,
            MAPBOX_TOLL_COLLECTIONS_SOURCE
        )
            .withProperties(
                *routeAlertViewOptions.properties.let {
                    if (it.isEmpty()) {
                        getMapboxRouteAlertSymbolLayerProperties()
                    } else {
                        it
                    }
                },
                PropertyFactory.iconImage(MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID),
                PropertyFactory.textField(
                    Expression.get(Expression.literal(MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID))
                )
            )

        routeAlertViewOptions.style.addImage(
            MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID,
            routeAlertViewOptions.drawable
                ?: ContextCompat.getDrawable(
                    routeAlertViewOptions.context,
                    R.drawable.mapbox_ic_route_alert_toll
                )!!
        )
        routeAlertViewOptions.style.addSource(tollCollectionsSource)
        routeAlertViewOptions.style.addLayer(tollCollectionsLayer)
    }

    companion object {
        private const val MAPBOX_TOLL_COLLECTIONS_SOURCE = "mapbox_toll_collections_source"
        private const val MAPBOX_TOLL_COLLECTIONS_LAYER = "mapbox_toll_collections_layer"
        private const val MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID =
            "mapbox_toll_collections_text_property_id"
        private const val MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID =
            "mapbox_toll_collections_image_property_id"
    }
}
