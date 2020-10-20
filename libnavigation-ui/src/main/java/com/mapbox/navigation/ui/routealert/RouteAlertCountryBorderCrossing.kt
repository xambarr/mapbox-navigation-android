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
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.generateSymbolLayerProperties

class RouteAlertCountryBorderCrossing(
    style: Style,
    countryBorderCrossingDrawable: Drawable,
    countryBorderCrossingProperties: Array<PropertyValue<out Any>>) {
    constructor(
        style: Style,
        context: Context
    ) : this(
        style = style,
        countryBorderCrossingDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.mapbox_ic_route_alert_country_border_crossing
        )!!,
        generateSymbolLayerProperties()
    )

    private val countryBorderCrossingSource = GeoJsonSource(MAPBOX_COUNTRY_BORDER_CROSSING_SOURCE)
    private val countryBorderCrossingLayer = SymbolLayer(
        MAPBOX_COUNTRY_BORDER_CROSSING_LAYER,
        MAPBOX_COUNTRY_BORDER_CROSSING_SOURCE
    ).withProperties(
        *countryBorderCrossingProperties,
        PropertyFactory.iconImage(MAPBOX_COUNTRY_BORDER_CROSSING_IMAGE_PROPERTY_ID),
        PropertyFactory.textField(
            Expression.get(Expression.literal(MAPBOX_COUNTRY_BORDER_CROSSING_TEXT_PROPERTY_ID))
        ))

    init {
        style.addImage(MAPBOX_COUNTRY_BORDER_CROSSING_IMAGE_PROPERTY_ID, countryBorderCrossingDrawable)
        style.addSource(countryBorderCrossingSource)
        style.addLayer(countryBorderCrossingLayer)
    }

    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<CountryBorderCrossingAlert>().run {
            val countryBorderCrossingFeatures = mutableListOf<Feature>()
            forEach { countryBorderCrossingAlert ->
                val from = countryBorderCrossingAlert.from
                val to = countryBorderCrossingAlert.to
                if (from != null && to != null) {
                    val feature = Feature.fromGeometry(countryBorderCrossingAlert.coordinate)
                    feature.addStringProperty(
                        MAPBOX_COUNTRY_BORDER_CROSSING_TEXT_PROPERTY_ID,
                        // TODO: define text format
                        "${from.codeAlpha3} -> ${to.codeAlpha3}")
                    countryBorderCrossingFeatures.add(feature)
                }
            }
            countryBorderCrossingSource.setGeoJson(
                FeatureCollection.fromFeatures(countryBorderCrossingFeatures)
            )
        }
    }

    companion object {
        const val MAPBOX_COUNTRY_BORDER_CROSSING_SOURCE = "mapbox_country_border_crossing_source"
        const val MAPBOX_COUNTRY_BORDER_CROSSING_LAYER = "mapbox_country_border_crossing_layer"
        const val MAPBOX_COUNTRY_BORDER_CROSSING_TEXT_PROPERTY_ID = "mapbox_country_border_crossing_text_property_id"
        const val MAPBOX_COUNTRY_BORDER_CROSSING_IMAGE_PROPERTY_ID = "mapbox_country_border_crossing_image_property_id"
    }
}
