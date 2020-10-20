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
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.generateSymbolLayerProperties

class RouteAlertToll(style: Style,
                     drawable: Drawable,
                     vararg properties: PropertyValue<out Any>) {
    constructor(style: Style,
                context: Context) : this(
        style = style,
        drawable = ContextCompat.getDrawable(context, R.drawable.mapbox_ic_route_alert_toll)!!,
        *generateSymbolLayerProperties())

    private val tollCollectionsSource = GeoJsonSource(MAPBOX_TOLL_COLLECTIONS_SOURCE)
    private val tollCollectionsLayer = SymbolLayer(MAPBOX_TOLL_COLLECTIONS_LAYER,
        MAPBOX_TOLL_COLLECTIONS_SOURCE)
        .withProperties(
            *properties,
            PropertyFactory.iconImage(MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID),
            PropertyFactory.textField(
                Expression.get(Expression.literal(MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID))
            )
        )

    init {
        style.addImage(MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID, drawable)
        style.addSource(tollCollectionsSource)
        style.addLayer(tollCollectionsLayer)
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<TollCollectionAlert>().run {
            val tollCollectionFeatures = mutableListOf<Feature>()
            forEach { tollCollectionAlert ->
                val typeString = when (tollCollectionAlert.tollCollectionType) {
                    TollCollectionType.TollGantry -> {
                        "toll gantry"
                    }
                    TollCollectionType.TollBooth -> {
                        "toll booth"
                    }
                    TollCollectionType.Unknown -> {
                        "unknown"
                    }
                    else -> {
                        return@forEach
                    }
                }

                val feature = Feature.fromGeometry(tollCollectionAlert.coordinate)
                feature.addStringProperty(
                    MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID,
                    typeString
                )
                tollCollectionFeatures.add(feature)
            }
            tollCollectionsSource.setGeoJson(
                FeatureCollection.fromFeatures(tollCollectionFeatures)
            )
        }
    }

    companion object {
        const val MAPBOX_TOLL_COLLECTIONS_SOURCE = "mapbox_toll_collections_source"
        const val MAPBOX_TOLL_COLLECTIONS_LAYER = "mapbox_toll_collections_layer"
        const val MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID = "mapbox_toll_collections_text_property_id"
        const val MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID = "mapbox_toll_collections_image_property_id"
    }
}
