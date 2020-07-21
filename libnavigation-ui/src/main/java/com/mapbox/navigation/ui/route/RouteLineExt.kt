package com.mapbox.navigation.ui.route

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.libnavigation.ui.R
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.ConcurrentHashMap


private interface MemoizedCall<in F, out R> {
    operator fun invoke(f: F): R
}

private class MemoizedHandler<F, in K : MemoizedCall<F, R>, out R>(val f: F) {
    private val m = ConcurrentHashMap<K, R>()
    operator fun invoke(k: K): R {
        return m[k] ?: run({
            val r = k(f)
            m.putIfAbsent(k, r)
            r
        })
    }
}

private data class MemoizeKey1<out P1, R>(val p1: P1) : MemoizedCall<(P1) -> R, R> {
    override fun invoke(f: (P1) -> R) = f(p1)
}

private data class MemoizeKey3<out P1, out P2, out P3, R>(val p1: P1, val p2: P2, val p3: P3) : MemoizedCall<(P1, P2, P3) -> R, R> {
    override fun invoke(f: (P1, P2, P3) -> R) = f(p1, p2, p3)
}

fun <P1, R> ((P1) -> R).memoize(): (P1) -> R {
    return object : (P1) -> R {
        private val m = MemoizedHandler<((P1) -> R), MemoizeKey1<P1, R>, R>(this@memoize)
        override fun invoke(p1: P1) = m(MemoizeKey1(p1))
    }
}

fun <P1, P2, P3, R> ((P1, P2, P3) -> R).memoize(): (P1, P2, P3) -> R {
    return object : (P1, P2, P3) -> R {
        private val m = MemoizedHandler<((P1, P2, P3) -> R), MemoizeKey3<P1, P2, P3, R>, R>(this@memoize)
        override fun invoke(p1: P1, p2: P2, p3: P3) = m(MemoizeKey3(p1, p2, p3))
    }
}

data class RouteLineExpressionData2(val offset: Float, val segmentColor: Int)

object RouteLineExt {

    val getLineStringForRoute = { route: DirectionsRoute -> LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6) }.memoize()

    val getRouteLineSegments = {
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

                // sometimes the fractional distance is returned in scientific notation
                // which the Maps Expression doesn't accept as valid input.
                // This checks that the value is above a certain threshold to prevent that.
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
}

class TrafficColorProvider(
    context: Context,
    @androidx.annotation.StyleRes styleRes: Int
) {
    val routeLineShieldTraveledColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeLineShieldTraveledColor,
            R.color.mapbox_navigation_route_shield_line_traveled_color,
            context,
            styleRes
        )
    }


    val routeUnknownColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeUnknownCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_unknown,
            context,
            styleRes
        )
    }

    val routeDefaultColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            context,
            styleRes
        )
    }

    val routeLowCongestionColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeLowCongestionColor,
            R.color.mapbox_navigation_route_traffic_layer_color,
            context,
            styleRes
        )
    }

    val routeModerateColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeModerateCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_yellow,
            context,
            styleRes
        )
    }

    val routeHeavyColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeHeavyCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_heavy,
            context,
            styleRes
        )
    }

    val routeSevereColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeSevereCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_red,
            context,
            styleRes
        )
    }

    val routeShieldColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_routeShieldColor,
            R.color.mapbox_navigation_route_shield_layer_color,
            context,
            styleRes
        )
    }

    val routeScale: Float by lazy {
        MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.NavigationMapRoute_routeScale,
            1.0f,
            context,
            styleRes
        )
    }

    val routeTrafficScale: Float by lazy {
        MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.NavigationMapRoute_routeTrafficScale,
            1.0f,
            context,
            styleRes
        )
    }

    val roundedLineCap: Boolean by lazy {
        MapRouteLine.MapRouteLineSupport.getBooleanStyledValue(
            R.styleable.NavigationMapRoute_roundedLineCap,
            true,
            context,
            styleRes
        )
    }

    val alternativeRouteUnknownColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteUnknownCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_unknown,
            context,
            styleRes
        )
    }

    val alternativeRouteDefaultColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteColor,
            R.color.mapbox_navigation_route_alternative_color,
            context,
            styleRes
        )
    }

    val alternativeRouteLowColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteLowCongestionColor,
            R.color.mapbox_navigation_route_alternative_color,
            context,
            styleRes
        )
    }

    val alternativeRouteModerateColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    val alternativeRouteHeavyColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteHeavyCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_heavy,
            context,
            styleRes
        )
    }

    val alternativeRouteSevereColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    val alternativeRouteShieldColor: Int by lazy {
        MapRouteLine.MapRouteLineSupport.getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteShieldColor,
            R.color.mapbox_navigation_route_alternative_shield_color,
            context,
            styleRes
        )
    }

    val alternativeRouteScale: Float by lazy {
        MapRouteLine.MapRouteLineSupport.getFloatStyledValue(
            R.styleable.NavigationMapRoute_alternativeRouteScale,
            1.0f,
            context,
            styleRes
        )
    }

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

