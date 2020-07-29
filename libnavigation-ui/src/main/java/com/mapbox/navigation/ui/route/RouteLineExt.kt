package com.mapbox.navigation.ui.route

import android.content.Context
import android.graphics.drawable.Drawable
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.libnavigation.ui.R
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.ui.internal.route.MapRouteSourceProvider
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider
import com.mapbox.navigation.ui.internal.utils.MapUtils
import com.mapbox.navigation.ui.internal.utils.memoize
import com.mapbox.navigation.ui.route.RouteLineExt.generateRouteFeatureCollection
import com.mapbox.navigation.ui.route.RouteLineExt.generateWaypointsFeatureCollection
import com.mapbox.navigation.ui.route.RouteLineExt.getExpressionAtOffset
import com.mapbox.navigation.ui.route.RouteLineExt.getRouteLineSegments
import com.mapbox.navigation.ui.route.RouteLineExt.getVanishRouteLineExpression
import com.mapbox.navigation.ui.route.RouteLineExt.setRouteLineSource
import com.mapbox.navigation.ui.route.RouteLineExt.updateRouteLine
import com.mapbox.turf.TurfMeasurement
import java.math.BigDecimal

data class RouteLineExpressionData2(val offset: Float, val segmentColor: Int)

object RouteLineExt {

    val getLineStringForRoute: (route: DirectionsRoute) -> LineString = {
            route: DirectionsRoute -> LineString.fromPolyline(route.geometry() ?: "", Constants.PRECISION_6)
        }.memoize()

    val generateRouteFeatureCollection: (route: DirectionsRoute) -> FeatureCollection = { route: DirectionsRoute ->
        val routeGeometry = getLineStringForRoute(route)
        val routeFeature = Feature.fromGeometry(routeGeometry)
        FeatureCollection.fromFeatures(listOf(routeFeature))
    }.memoize()

    val generateWaypointsFeatureCollection: (route: DirectionsRoute) -> FeatureCollection = { route: DirectionsRoute ->
        val wayPointFeatures = mutableListOf<Feature>()
        route.legs()?.forEach {
            MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(it, 0)?.let { feature ->
                wayPointFeatures.add(feature)
            }

            it.steps()?.let { steps ->
                MapRouteLine.MapRouteLineSupport.buildWayPointFeatureFromLeg(it, steps.lastIndex)?.let { feature ->
                    wayPointFeatures.add(feature)
                }
            }
        }
        FeatureCollection.fromFeatures(wayPointFeatures)
    }.memoize()

    val getRouteLineSegments: (
        route: DirectionsRoute,
        isPrimaryRoute: Boolean,
        congestionColorProvider: (String, Boolean) -> Int
    ) -> List<RouteLineExpressionData2> = {
        route: DirectionsRoute,
        isPrimaryRoute: Boolean,
        congestionColorProvider: (String, Boolean) -> Int ->

        val congestionSections = route.legs()
            ?.map { it.annotation()?.congestion() ?: listOf() }
            ?.flatten() ?: listOf()

        when (congestionSections.isEmpty()) {
            false -> calculateRouteLineSegmentsFromCongestion(
                congestionSections,
                getLineStringForRoute(route),
                route.distance() ?: 0.0,
                isPrimaryRoute,
                congestionColorProvider
            )
            true -> listOf(
                RouteLineExpressionData2(
                    0f,
                    congestionColorProvider("", isPrimaryRoute)
                )
            )
        }
    }.memoize()

    fun calculateRouteLineSegmentsFromCongestion(
        congestionSections: List<String>,
        lineString: LineString,
        routeDistance: Double,
        isPrimary: Boolean,
        congestionColorProvider: (String, Boolean) -> Int
    ): List<RouteLineExpressionData2> {
        val expressionStops = mutableListOf<RouteLineExpressionData2>()
        val numCongestionPoints: Int = congestionSections.size
        var previousCongestion = ""
        var distanceTraveled = 0.0
        for (i in 0 until numCongestionPoints) {
            if (i + 1 < lineString.coordinates().size) {
                distanceTraveled += (TurfMeasurement.distance(
                    lineString.coordinates()[i],
                    lineString.coordinates()[i + 1]
                ) * 1000)

                if (congestionSections[i] == previousCongestion) {
                    continue
                }

                val fractionalDist: Double = distanceTraveled / routeDistance
                if (fractionalDist < RouteConstants.MINIMUM_ROUTE_LINE_OFFSET) {
                    continue
                }

                if (expressionStops.isEmpty()) {
                    expressionStops.add(
                        RouteLineExpressionData2(
                            0f,
                            congestionColorProvider(congestionSections[i], isPrimary)
                        )
                    )
                }
                val routeColor = congestionColorProvider(congestionSections[i], isPrimary)
                expressionStops.add(
                    RouteLineExpressionData2(
                        fractionalDist.toFloat(),
                        routeColor
                    )
                )
                previousCongestion = congestionSections[i]
            }
        }
        if (expressionStops.isEmpty()) {
            expressionStops.add(
                RouteLineExpressionData2(
                    0f,
                    congestionColorProvider("", isPrimary)
                )
            )
        }
        return expressionStops
    }

    fun updateRouteLine(expression: Expression, layerId:String, style: Style) {
        if (style.isFullyLoaded) {
            style.getLayer(layerId)?.setProperties(
                PropertyFactory.lineGradient(
                    expression
                )
            )
        }
    }

    fun getExpressionAtOffset(
        distanceOffset: Float,
        routeLineExpressionData: List<RouteLineExpressionData2>,
        trafficColorProvider: RouteLineColorProvider): Expression {
        val filteredItems = routeLineExpressionData.filter { it.offset > distanceOffset }
        val trafficExpressions = when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(RouteLineExpressionData2(distanceOffset, trafficColorProvider.routeUnknownColor))
                false -> listOf(routeLineExpressionData.last().copy(offset = distanceOffset))
            }
            false -> {
                val firstItemIndex = routeLineExpressionData.indexOf(filteredItems.first())
                val fillerItem = if (firstItemIndex == 0) {
                    routeLineExpressionData[firstItemIndex]
                } else {
                    routeLineExpressionData[firstItemIndex - 1]
                }
                listOf(fillerItem.copy(offset = distanceOffset)).plus(filteredItems)
            }
        }.map {
            Expression.stop(
                it.offset.toBigDecimal().setScale(9, BigDecimal.ROUND_DOWN),
                Expression.color(it.segmentColor)
            )
        }

        return Expression.step(
            Expression.lineProgress(),
            Expression.rgba(0, 0, 0, 0),
            *trafficExpressions.toTypedArray()
        )
    }

    fun initializeRouteLineMapSources(style: Style, mapRouteSourceProvider: MapRouteSourceProvider) {
        if (style.isFullyLoaded) {
            val wayPointGeoJsonOptions = GeoJsonOptions().withMaxZoom(16)
            addSourceIfAbsent(RouteConstants.WAYPOINT_SOURCE_ID, wayPointGeoJsonOptions, style, mapRouteSourceProvider)

            val routeLineGeoJsonOptions = GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
            addSourceIfAbsent(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, routeLineGeoJsonOptions, style, mapRouteSourceProvider)

            val routeLineTrafficGeoJsonOptions = GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
            addSourceIfAbsent(RouteConstants.PRIMARY_ROUTE_TRAFFIC_SOURCE_ID, routeLineTrafficGeoJsonOptions, style, mapRouteSourceProvider)

            val alternativeRouteLineGeoJsonOptions =
                GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
            addSourceIfAbsent(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID, alternativeRouteLineGeoJsonOptions, style, mapRouteSourceProvider)
        }
    }

    fun addSourceIfAbsent(
        sourceId: String,
        sourceOptions: GeoJsonOptions,
        style: Style,
        mapRouteSourceProvider: MapRouteSourceProvider) {
        if (style.getSource(sourceId) == null) {
            val source = mapRouteSourceProvider.build(
                RouteConstants.WAYPOINT_SOURCE_ID,
                FeatureCollection.fromFeatures(listOf()),
                sourceOptions
            )
            style.addSource(source)
        }
    }

    fun setRouteLineSource(sourceId: String, style: Style, featureCollection: FeatureCollection) {
        if (style.isFullyLoaded) {
            style.getSourceAs<GeoJsonSource>(sourceId)?.setGeoJson(featureCollection)
        }
    }

    fun initializeLayers(
        style: Style,
        layerProvider: RouteLayerProvider,
        originIcon: Drawable,
        destinationIcon: Drawable,
        belowLayerId: String,
        routeLineColorProvider: RouteLineColorProvider
    ) {
        layerProvider.initializeAlternativeRouteShieldLayer(
            style,
            routeLineColorProvider.alternativeRouteScale,
            routeLineColorProvider.alternativeRouteShieldColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }

        layerProvider.initializeAlternativeRouteLayer(
            style,
            routeLineColorProvider.roundedLineCap,
            routeLineColorProvider.alternativeRouteScale,
            routeLineColorProvider.alternativeRouteDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }

        layerProvider.initializePrimaryRouteShieldLayer(
            style,
            routeLineColorProvider.routeScale,
            routeLineColorProvider.routeShieldColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }

        layerProvider.initializePrimaryRouteLayer(
            style,
            routeLineColorProvider.roundedLineCap,
            routeLineColorProvider.routeScale,
            routeLineColorProvider.routeDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }

        layerProvider.initializePrimaryRouteTrafficLayer(
            style,
            routeLineColorProvider.roundedLineCap,
            routeLineColorProvider.routeTrafficScale,
            routeLineColorProvider.routeDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }

        layerProvider.initializeWayPointLayer(
            style, originIcon, destinationIcon
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
        }
    }

    fun getVanishRouteLineExpression(offset: Float, traveledColor: Int, defaultColor: Int): Expression {
        return Expression.step(
            Expression.lineProgress(),
            Expression.color(traveledColor),
            Expression.stop(
                offset.toBigDecimal().setScale(9, BigDecimal.ROUND_DOWN),
                Expression.color(defaultColor)
            )
        )
    }
}

data class MapRouteLineState(
    val vanishPointOffset: Float = 0f,
    val routes: List<DirectionsRoute> = listOf()
)

interface MapRouteLineAPI {
    fun draw(directionsRoute: DirectionsRoute)
    fun draw(directionsRoutes: List<DirectionsRoute>)
    fun hideRouteLineAtOffset(offset: Float)
    fun decorateRouteLine(expression: Expression)
    fun hideShieldLineAtOffset(offset: Float)
}

class MapRouteLine2(
    private val mapRouteLineState: MapRouteLineState = MapRouteLineState(),
    private val style: Style,
    private val trafficColorProvider: RouteLineColorProvider
    ): MapRouteLineAPI {

    private var currentRouteLineState = mapRouteLineState


    override fun draw(directionsRoute: DirectionsRoute) {
        draw(listOf(directionsRoute))
    }

    override fun draw(directionsRoutes: List<DirectionsRoute>) {
        reset()
        //todo
    }

    // todo maybe this shouldn't be here
    override fun hideRouteLineAtOffset(offset: Float) {
        val expression = getVanishRouteLineExpression(
            offset,
            trafficColorProvider.routeLineTraveledColor,
            trafficColorProvider.routeDefaultColor
        )
        updateRouteLine(expression, RouteConstants.PRIMARY_ROUTE_LAYER_ID, style)
    }

    // todo maybe this shouldn't be here
    override fun hideShieldLineAtOffset(offset: Float) {
        val expression = getVanishRouteLineExpression(
            offset,
            trafficColorProvider.routeLineShieldTraveledColor,
            trafficColorProvider.routeShieldColor
        )
        updateRouteLine(expression, RouteConstants.PRIMARY_ROUTE_SHIELD_LAYER_ID, style)
    }

    private fun hideTrafficLineAtOffset(offset: Float, directionsRoute: DirectionsRoute) {
        val segments = getRouteLineSegments(directionsRoute, true, trafficColorProvider::getRouteColorForCongestion)
        val expression = getExpressionAtOffset(offset, segments, trafficColorProvider)
        updateRouteLine(expression, RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, style)
    }

    // todo maybe this shouldn't be here
    override fun decorateRouteLine(expression: Expression)=
        updateRouteLine(expression, RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, style)

    private fun drawAlternativeRoutes(routes: List<DirectionsRoute>) {
        routes.mapNotNull {
            generateRouteFeatureCollection(it).features()
        }.flatten().let {
            setRouteLineSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID, style, FeatureCollection.fromFeatures(it))
        }
    }

    private fun drawPrimaryRoute(directionsRoute: DirectionsRoute) {
        val featureCollection =  generateRouteFeatureCollection(directionsRoute)
        setRouteLineSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, style, featureCollection)
        setRouteLineSource(RouteConstants.PRIMARY_ROUTE_TRAFFIC_SOURCE_ID, style, featureCollection)
        hideRouteLineAtOffset(mapRouteLineState.vanishPointOffset)
        hideShieldLineAtOffset(mapRouteLineState.vanishPointOffset)
        hideTrafficLineAtOffset(mapRouteLineState.vanishPointOffset, directionsRoute)
    }

    private fun reset() {
        currentRouteLineState = MapRouteLineState(0f, listOf())
        setRouteLineSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, style, FeatureCollection.fromFeatures(arrayOf()))
        setRouteLineSource(RouteConstants.PRIMARY_ROUTE_TRAFFIC_SOURCE_ID, style, FeatureCollection.fromFeatures(arrayOf()))
        setRouteLineSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID, style, FeatureCollection.fromFeatures(arrayOf()))
        setRouteLineSource(RouteConstants.WAYPOINT_SOURCE_ID, style, FeatureCollection.fromFeatures(arrayOf()))
    }

    internal fun drawWayPoints(directionsRoute: DirectionsRoute) {
        val featureCollection = generateWaypointsFeatureCollection(directionsRoute)
        setRouteLineSource(RouteConstants.WAYPOINT_SOURCE_ID, style, featureCollection)
    }
}

object RouteLineColorProviderFactory {
    fun getRouteLineColorProvider(
        context: Context,
        @androidx.annotation.StyleRes styleRes: Int
    ): RouteLineColorProvider {
        return object : RouteLineColorProvider {
            override val routeLineTraveledColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeLineTraveledColor,
                    R.color.mapbox_navigation_route_line_traveled_color,
                    context,
                    styleRes
                )
            override val routeLineShieldTraveledColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeLineShieldTraveledColor,
                    R.color.mapbox_navigation_route_shield_line_traveled_color,
                    context,
                    styleRes
                )
            override val routeUnknownColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeUnknownCongestionColor,
                    R.color.mapbox_navigation_route_layer_congestion_unknown,
                    context,
                    styleRes
                )
            override val routeDefaultColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeColor,
                    R.color.mapbox_navigation_route_layer_blue,
                    context,
                    styleRes
                )
            override val routeLowCongestionColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeLowCongestionColor,
                    R.color.mapbox_navigation_route_traffic_layer_color,
                    context,
                    styleRes
                )
            override val routeModerateColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeModerateCongestionColor,
                    R.color.mapbox_navigation_route_layer_congestion_yellow,
                    context,
                    styleRes
                )
            override val routeHeavyColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeHeavyCongestionColor,
                    R.color.mapbox_navigation_route_layer_congestion_heavy,
                    context,
                    styleRes
                )
            override val routeSevereColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeSevereCongestionColor,
                    R.color.mapbox_navigation_route_layer_congestion_red,
                    context,
                    styleRes
                )
            override val routeShieldColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_routeShieldColor,
                    R.color.mapbox_navigation_route_shield_layer_color,
                    context,
                    styleRes
                )
            override val routeScale: Float
                get() = MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
                    R.styleable.NavigationMapRoute_routeScale,
                    1.0f,
                    context,
                    styleRes
                )
            override val routeTrafficScale: Float
                get() = MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
                    R.styleable.NavigationMapRoute_routeTrafficScale,
                    1.0f,
                    context,
                    styleRes
                )
            override val roundedLineCap: Boolean
                get() = MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
                    R.styleable.NavigationMapRoute_roundedLineCap,
                    true,
                    context,
                    styleRes
                )
            override val alternativeRouteUnknownColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteLowCongestionColor,
                    R.color.mapbox_navigation_route_alternative_color,
                    context,
                    styleRes
                )
            override val alternativeRouteDefaultColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteLowCongestionColor,
                    R.color.mapbox_navigation_route_alternative_color,
                    context,
                    styleRes
                )
            override val alternativeRouteLowColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteLowCongestionColor,
                    R.color.mapbox_navigation_route_alternative_color,
                    context,
                    styleRes
                )
            override val alternativeRouteModerateColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
                    R.color.mapbox_navigation_route_alternative_congestion_red,
                    context,
                    styleRes
                )
            override val alternativeRouteHeavyColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteHeavyCongestionColor,
                    R.color.mapbox_navigation_route_alternative_congestion_heavy,
                    context,
                    styleRes
                )
            override val alternativeRouteSevereColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
                    R.color.mapbox_navigation_route_alternative_congestion_red,
                    context,
                    styleRes
                )
            override val alternativeRouteShieldColor: Int
                get() = MapRouteLine.MapRouteLineSupport.getStyledColor(
                    R.styleable.NavigationMapRoute_alternativeRouteShieldColor,
                    R.color.mapbox_navigation_route_alternative_shield_color,
                    context,
                    styleRes
                )
            override val alternativeRouteScale: Float
                get() = MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
                    R.styleable.NavigationMapRoute_alternativeRouteScale,
                    1.0f,
                    context,
                    styleRes
                )
        }
    }
}

interface RouteLineColorProvider {
    val routeLineTraveledColor: Int
    val routeLineShieldTraveledColor: Int
    val routeUnknownColor: Int
    val routeDefaultColor: Int
    val routeLowCongestionColor: Int
    val routeModerateColor: Int
    val routeHeavyColor: Int
    val routeSevereColor: Int
    val routeShieldColor: Int
    val routeScale: Float
    val routeTrafficScale: Float
    val roundedLineCap: Boolean
    val alternativeRouteUnknownColor: Int
    val alternativeRouteDefaultColor: Int
    val alternativeRouteLowColor: Int
    val alternativeRouteModerateColor: Int
    val alternativeRouteHeavyColor: Int
    val alternativeRouteSevereColor: Int
    val alternativeRouteShieldColor: Int
    val alternativeRouteScale: Float

    fun getRouteColorForCongestion(congestionValue: String, isPrimaryRoute: Boolean): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                RouteConstants.MODERATE_CONGESTION_VALUE -> routeModerateColor
                RouteConstants.HEAVY_CONGESTION_VALUE -> routeHeavyColor
                RouteConstants.SEVERE_CONGESTION_VALUE -> routeSevereColor
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> routeUnknownColor
                else -> routeLowCongestionColor
            }
            false -> when (congestionValue) {
                RouteConstants.MODERATE_CONGESTION_VALUE -> alternativeRouteModerateColor
                RouteConstants.HEAVY_CONGESTION_VALUE -> alternativeRouteHeavyColor
                RouteConstants.SEVERE_CONGESTION_VALUE -> alternativeRouteSevereColor
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> alternativeRouteUnknownColor
                else -> alternativeRouteDefaultColor
            }
        }
    }
}


