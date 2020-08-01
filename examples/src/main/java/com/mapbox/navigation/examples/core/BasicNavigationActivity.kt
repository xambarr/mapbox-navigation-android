package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.Utils.PRIMARY_ROUTE_BUNDLE_KEY
import com.mapbox.navigation.examples.utils.Utils.getRouteFromBundle
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.internal.ThemeSwitcher
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_SHIELD_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimator
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimatorHandler
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import com.mapbox.navigation.ui.puck.PuckDrawableSupplier
import com.mapbox.navigation.ui.route.RouteLineColorProvider
import com.mapbox.navigation.ui.route.RouteLineColorProviderFactory.getRouteLineColorProvider
import com.mapbox.navigation.ui.route.RouteLineExt.updateRouteLine
import com.mapbox.navigation.ui.route.findDistanceOfPointAlongLine
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.math.BigDecimal
import kotlin.math.abs

/**
 * This activity shows how to set up a basic turn-by-turn
 * navigation experience with the Navigation SDK and
 * Navigation UI SDK.
 */
open class BasicNavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state"
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null
    private val mapboxReplayer = MapboxReplayer()
    private var directionRoute: DirectionsRoute? = null
    private var lineString: LineString? = null

    lateinit var colorProvider: RouteLineColorProvider

    private val mapStyles = listOf(
        Style.MAPBOX_STREETS,
        Style.OUTDOORS,
        Style.LIGHT,
        Style.DARK,
        Style.SATELLITE_STREETS
    )

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val routeStyleRes = ThemeSwitcher.retrieveAttrResourceId(this, R.attr.navigationViewRouteStyle, R.style.NavigationMapRoute)
        colorProvider = getRouteLineColorProvider(this, routeStyleRes)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(getLocationEngine())
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerLocationObserver(locationObserver)
        }

        initListeners()

        vanishingRouteLineAnimator.valueAnimatorHandler = routeLineAnimatorUpdateHandler
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, null, false, true).also {
                it.setPuckDrawableSupplier(CustomPuckDrawableSupplier())
            }
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }

            //when (directionRoute) {
                //null -> {
                    if (shouldSimulateRoute()) {
                        mapboxNavigation?.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                        mapboxReplayer.pushRealLocation(this, 0.0)
                        mapboxReplayer.play()
                    }
                    mapboxNavigation?.navigationOptions?.locationEngine?.getLastLocation(locationListenerCallback)
                    Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                        .show()
                //}
                //else -> restoreNavigation()
            //}
        }
        mapboxMap.addOnMapLongClickListener { latLng ->
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }
    }

    var locationCollection = mutableListOf<Point>()
    var distances = mutableListOf<Float>()
    private val locationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (locationCollection.size > 0) {
                val distance = TurfMeasurement.distance(enhancedLocation.toPoint(), locationCollection.last(), UNIT_METERS)
                distances.add(distance.toFloat())
            }
            //locationCollection.add(enhancedLocation.toPoint())
            //
            // val distanceTraveled = distances.sum()
            // val percentTraveled = distanceTraveled / directionRoute!!.distance()!!
            //
            // Timber.e("*** location update distance traveled ${distanceTraveled.toBigDecimal()}")
            // Timber.i("*** location update percentage traveled: ${percentTraveled.toBigDecimal()}")
            //
            // getAnimatorFun(
            //     directionRoute!!.distance()!!.toFloat(),
            //     distances,
            //     getExpressionProvider(colorProvider.routeLineTraveledColor, colorProvider.routeDefaultColor),
            //     getUpdateRouteLineFun(navigationMapboxMap!!.retrieveMap().style!!, listOf(PRIMARY_ROUTE_LAYER_ID, PRIMARY_ROUTE_SHIELD_LAYER_ID, PRIMARY_ROUTE_TRAFFIC_LAYER_ID))
            // ).invoke(enhancedLocation.toPoint())

            //jobControl.scope.launch {
                // val distanceTraveledLegacy = distances.sum()
                // val percentTraveledLegacy = distanceTraveledLegacy / directionRoute!!.distance()!!
                //
                // Timber.e("*** location update distance traveled legacy ${distanceTraveledLegacy.toBigDecimal()}")
                // Timber.i("*** location update percentage traveled legacy ${percentTraveledLegacy.toBigDecimal()}")

                val distanceTraveled = findDistanceOfPointAlongLine(lineString!!, enhancedLocation.toPoint())
                val percentTraveled = distanceTraveled / directionRoute!!.distance()!!

                Timber.e("*** location update distance traveled ${distanceTraveled.toBigDecimal()}")
                Timber.e("*** location update percent distance traveled ${percentTraveled.toBigDecimal()}")

                // val distanceDiff = abs(distanceTraveledLegacy - distanceTraveled)
                // val percentDiff = abs(percentTraveledLegacy - percentTraveled)
                // Timber.e("*** distance difference of ${distanceDiff.toBigDecimal()} percent difference ${percentDiff.toBigDecimal()}")

                vanishingRouteLineAnimator.cancelAnimationCallbacks()
                vanishingRouteLineAnimator.start(lastDistanceValue, percentTraveled.toFloat())
            //}

            // val expressionProvider = getExpressionProvider(colorProvider.routeLineTraveledColor, colorProvider.routeDefaultColor)
            // val expression = expressionProvider.invoke(percentTraveled.toFloat())

        }

        override fun onRawLocationChanged(rawLocation: Location) {

        }
    }

    private val jobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    var lastDistanceValue = 0f
    val routeLineAnimatorUpdateHandler: RouteLineValueAnimatorHandler = { animationValue ->
        if (animationValue > RouteConstants.MINIMUM_ROUTE_LINE_OFFSET) {
            val expression = getExpressionProvider(colorProvider.routeLineTraveledColor, colorProvider.routeDefaultColor)(animationValue)
            updateRouteLine(expression, PRIMARY_ROUTE_LAYER_ID, navigationMapboxMap!!.retrieveMap().style!!)
            updateRouteLine(expression, PRIMARY_ROUTE_SHIELD_LAYER_ID, navigationMapboxMap!!.retrieveMap().style!!)
            updateRouteLine(expression, PRIMARY_ROUTE_TRAFFIC_LAYER_ID, navigationMapboxMap!!.retrieveMap().style!!)
            lastDistanceValue = animationValue
        }
    }


    var vanishingRouteLineAnimator = RouteLineValueAnimator()

    fun getAnimatorFun(
        totalDistance: Float,
        intermediateDistances: MutableList<Float>,
        expressionProvider: (Float) -> Expression,
        updateRouteLineFun: (Expression) -> Unit
    ): (Point) -> Unit = { point ->
        intermediateDistances.add(TurfMeasurement.distance(point, locationCollection.last(), UNIT_METERS).toFloat())
        val updatedDistanceTraveled = distances.sum()
        val percentTraveled = updatedDistanceTraveled / totalDistance
        val expression = expressionProvider(percentTraveled.toFloat())
        updateRouteLineFun(expression)
    }

    fun getExpressionProvider(traveledColor: Int, defaultColor: Int): (Float) -> Expression = { offset ->
        Expression.step(
            Expression.lineProgress(),
            Expression.color(traveledColor),
            Expression.stop(
                offset.toBigDecimal().setScale(9, BigDecimal.ROUND_DOWN),
                Expression.color(defaultColor)
            )
        )
    }

    fun getUpdateRouteLineFun(style: Style, layerIds: List<String>): (Expression) -> Unit = { expression ->
        if (style.isFullyLoaded) {
            layerIds.forEach { layerId ->
                style.getLayer(layerId)?.setProperties(
                    PropertyFactory.lineGradient(
                        expression
                    )
                )
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            // do something with the route progress
            Timber.i("*** route progress distance traveled ${routeProgress.distanceTraveled.toBigDecimal()}")
            Timber.i("*** route progress percentage traveled: ${getPercentDistanceTraveled(routeProgress).toBigDecimal()}")
        }
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDist =
            (routeProgress.distanceRemaining + routeProgress.distanceTraveled)
        return routeProgress.distanceTraveled / totalDist
    }

    fun startLocationUpdates() {
        if (!shouldSimulateRoute()) {
            val requestLocationUpdateRequest =
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                    .build()

            mapboxNavigation?.navigationOptions?.locationEngine?.requestLocationUpdates(
                requestLocationUpdateRequest,
                locationListenerCallback,
                mainLooper
            )
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                directionRoute = routes[0]
                lineString = LineString.fromPolyline(mapboxNavigation?.getRoutes()!![0].geometry()!!, Constants.PRECISION_6)
                navigationMapboxMap?.drawRoute(routes[0])
                startNavigation.visibility = View.VISIBLE
            } else {
                startNavigation.visibility = View.GONE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    @SuppressLint("MissingPermission")
    fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])


                // LineString.fromPolyline(mapboxNavigation?.getRoutes()!![0].geometry()!!, Constants.PRECISION_6).coordinates().forEach {
                //     Timber.e("*** point: ${it.latitude()} ${it.longitude()}")
                // }
                val routeCoordinates = LineString.fromPolyline(mapboxNavigation?.getRoutes()!![0].geometry()!!, Constants.PRECISION_6).coordinates()
                locationCollection.add(routeCoordinates.first())


            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
            stopLocationUpdates()
        }

        fabToggleStyle.setOnClickListener {
            navigationMapboxMap?.retrieveMap()?.setStyle(mapStyles.shuffled().first())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        vanishingRouteLineAnimator.cancelAnimationCallbacks()
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxReplayer.finish()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationMapboxMap?.saveStateWith(MAP_INSTANCE_STATE_KEY, outState)
        mapView.onSaveInstanceState(outState)

        // This is not the most efficient way to preserve the route on a device rotation.
        // This is here to demonstrate that this event needs to be handled in order to
        // redraw the route line after a rotation.
        directionRoute?.let {
            outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mapInstanceState = savedInstanceState?.getParcelable(MAP_INSTANCE_STATE_KEY)
        directionRoute = getRouteFromBundle(savedInstanceState)
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private fun stopLocationUpdates() {
        if (!shouldSimulateRoute()) {
            mapboxNavigation?.navigationOptions?.locationEngine?.removeLocationUpdates(locationListenerCallback)
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    navigationMapboxMap?.removeRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    // Used to determine if the ReplayRouteLocationEngine should be used to simulate the routing.
    // This is used for testing purposes.
    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    // If shouldSimulateRoute is true a ReplayRouteLocationEngine will be used which is intended
    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute()) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    private class MyLocationEngineCallback(activity: BasicNavigationActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.i(exception)
        }
    }

    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        directionRoute?.let {
            mapboxNavigation?.setRoutes(listOf(it))
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation?.startTripSession()
        }
    }

    // private fun getDirectionsRoute(): DirectionsRoute {
    //     return DirectionsRoute.fromJson(routeToUse)
    // }


    val routeToUse = "{\"routeIndex\":\"0\",\"distance\":879.1,\"duration\":228.6,\"geometry\":\"miylgAniguhF{Cra@iBdVa@nFtThE`RpDpFfBr]xEvCd@nU~DUbCoBnd@vn@lC~EVzRj@jOfA~Rr@iAbQiBh^o@|N[fSlUjBbPpAnTfB|FeiA\",\"weight\":396.4,\"weight_name\":\"routability\",\"legs\":[{\"distance\":879.1,\"duration\":228.6,\"summary\":\"Nye Street, Lootens Place\",\"steps\":[{\"distance\":93.1,\"duration\":24.4,\"geometry\":\"miylgAniguhF{Cra@iBdVa@nF\",\"name\":\"Laurel Place\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523816,37.975207],\"bearing_before\":0.0,\"bearing_after\":280.0,\"instruction\":\"Head west on Laurel Place\",\"type\":\"depart\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":93.1,\"announcement\":\"Head west on Laurel Place, then turn left onto Nye Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead west on Laurel Place, then turn left onto Nye Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":57.2,\"announcement\":\"Turn left onto Nye Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Nye Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":93.1,\"primary\":{\"text\":\"Nye Street\",\"components\":[{\"text\":\"Nye Street\",\"type\":\"text\",\"abbr\":\"Nye St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":54.9,\"intersections\":[{\"location\":[-122.523816,37.975207],\"bearings\":[280],\"entry\":[true],\"out\":0}]},{\"distance\":193.5,\"duration\":57.7,\"geometry\":\"urylgAxjiuhFtThE`RpDpFfBr]xEvCd@nU~D\",\"name\":\"Nye Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.524861,37.975355],\"bearing_before\":279.0,\"bearing_after\":192.0,\"instruction\":\"Turn left onto Nye Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":173.5,\"announcement\":\"In 600 feet, turn right onto 5th Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 600 feet, turn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e5th\\u003c/say-as\\u003e Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":50.3,\"announcement\":\"Turn right onto 5th Avenue, then turn left onto Lootens Place\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e5th\\u003c/say-as\\u003e Avenue, then turn left onto Lootens Place\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":193.5,\"primary\":{\"text\":\"5th Avenue\",\"components\":[{\"text\":\"5th Avenue\",\"type\":\"text\",\"abbr\":\"5th Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":50.3,\"primary\":{\"text\":\"5th Avenue\",\"components\":[{\"text\":\"5th Avenue\",\"type\":\"text\",\"abbr\":\"5th Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"},\"sub\":{\"text\":\"Lootens Place\",\"components\":[{\"text\":\"Lootens Place\",\"type\":\"text\",\"abbr\":\"Lootens Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":70.5,\"intersections\":[{\"location\":[-122.524861,37.975355],\"bearings\":[15,105,195,285],\"entry\":[true,false,true,true],\"in\":1,\"out\":2},{\"location\":[-122.525103,37.974582],\"bearings\":[15,105,195,285],\"entry\":[false,true,true,true],\"in\":0,\"out\":2}]},{\"distance\":58.9,\"duration\":13.1,\"geometry\":\"ohvlgA|gjuhFUbCoBnd@\",\"name\":\"5th Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.525327,37.973656],\"bearing_before\":191.0,\"bearing_after\":281.0,\"instruction\":\"Turn right onto 5th Avenue\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":58.9,\"announcement\":\"Turn left onto Lootens Place\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Lootens Place\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":58.9,\"primary\":{\"text\":\"Lootens Place\",\"components\":[{\"text\":\"Lootens Place\",\"type\":\"text\",\"abbr\":\"Lootens Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":47.3,\"intersections\":[{\"location\":[-122.525327,37.973656],\"bearings\":[15,105,180,285],\"entry\":[false,true,true,true],\"in\":0,\"out\":3}]},{\"distance\":198.1,\"duration\":60.8,\"geometry\":\"ulvlgApqkuhFvn@lC~EVzRj@jOfA~Rr@\",\"name\":\"Lootens Place\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.525993,37.973723],\"bearing_before\":275.0,\"bearing_after\":182.0,\"instruction\":\"Turn left onto Lootens Place\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":178.1,\"announcement\":\"In 600 feet, turn right onto 3rd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 600 feet, turn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":48.9,\"announcement\":\"Turn right onto 3rd Street, then turn left onto Brooks Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street, then turn left onto Brooks Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":198.1,\"primary\":{\"text\":\"3rd Street\",\"components\":[{\"text\":\"3rd Street\",\"type\":\"text\",\"abbr\":\"3rd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":48.9,\"primary\":{\"text\":\"3rd Street\",\"components\":[{\"text\":\"3rd Street\",\"type\":\"text\",\"abbr\":\"3rd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"},\"sub\":{\"text\":\"Brooks Street\",\"components\":[{\"text\":\"Brooks Street\",\"type\":\"text\",\"abbr\":\"Brooks St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":89.2,\"intersections\":[{\"location\":[-122.525993,37.973723],\"bearings\":[15,90,180,300],\"entry\":[true,false,true,true],\"in\":1,\"out\":2},{\"location\":[-122.526064,37.972959],\"bearings\":[0,105,180,270],\"entry\":[false,true,true,true],\"in\":0,\"out\":2},{\"location\":[-122.526098,37.972529],\"bearings\":[0,105,180],\"entry\":[false,true,true],\"in\":0,\"out\":2}]},{\"distance\":121.0,\"duration\":15.2,\"geometry\":\"u}rlgA~{kuhFiAbQiBh^o@|N[fS\",\"name\":\"3rd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.52616,37.971947],\"bearing_before\":182.0,\"bearing_after\":278.0,\"instruction\":\"Turn right onto 3rd Street\",\"type\":\"end of road\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":119.4,\"announcement\":\"Turn left onto Brooks Street, then turn left onto 2nd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Brooks Street, then turn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e2nd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":121.0,\"primary\":{\"text\":\"Brooks Street\",\"components\":[{\"text\":\"Brooks Street\",\"type\":\"text\",\"abbr\":\"Brooks St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":119.4,\"primary\":{\"text\":\"Brooks Street\",\"components\":[{\"text\":\"Brooks Street\",\"type\":\"text\",\"abbr\":\"Brooks St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"2nd Street\",\"components\":[{\"text\":\"2nd Street\",\"type\":\"text\",\"abbr\":\"2nd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":45.2,\"intersections\":[{\"location\":[-122.52616,37.971947],\"bearings\":[0,105,285],\"entry\":[false,false,true],\"in\":0,\"out\":2}]},{\"distance\":109.4,\"duration\":49.9,\"geometry\":\"ueslgArqnuhFlUjBbPpAnTfB\",\"name\":\"Brooks Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.52753,37.972075],\"bearing_before\":272.0,\"bearing_after\":185.0,\"instruction\":\"Turn left onto Brooks Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":32.9,\"announcement\":\"Turn left onto 2nd Street, then you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e2nd\\u003c/say-as\\u003e Street, then you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":109.4,\"primary\":{\"text\":\"2nd Street\",\"components\":[{\"text\":\"2nd Street\",\"type\":\"text\",\"abbr\":\"2nd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":81.8,\"intersections\":[{\"location\":[-122.52753,37.972075],\"bearings\":[90,180,270],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":105.0,\"duration\":7.5,\"geometry\":\"shqlgAxznuhF|FeiA\",\"name\":\"2nd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.527677,37.971098],\"bearing_before\":185.0,\"bearing_after\":97.0,\"instruction\":\"Turn left onto 2nd Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":70.0,\"announcement\":\"You have arrived at your destination, on the right\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination, on the right\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":105.0,\"primary\":{\"text\":\"You will arrive\",\"components\":[{\"text\":\"You will arrive\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":70.0,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":7.5,\"intersections\":[{\"location\":[-122.527677,37.971098],\"bearings\":[0,105,270],\"entry\":[false,true,false],\"in\":0,\"out\":1}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"u`qlgArpluhF\",\"name\":\"2nd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.52649,37.970971],\"bearing_before\":98.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination, on the right\",\"type\":\"arrive\",\"modifier\":\"right\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.52649,37.970971],\"bearings\":[278],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[49.34180914849393,33.05802569090612,10.689795908138624,39.59839199650681,34.80992351675273,14.209672171473654,55.332462814681335,8.615785848371143,40.91659557025385,5.914738766868038,52.97482177741399,85.20461216655733,12.501699782507817,35.42252405667138,29.311743333674485,35.66534898263901,25.758372926868166,44.32194001964289,22.517423451854935,28.451254156899267,40.20997808786729,30.687302775025532,38.532552346674045,105.03289645548367],\"congestion\":[\"low\",\"unknown\",\"unknown\",\"low\",\"unknown\",\"unknown\",\"heavy\",\"low\",\"low\",\"unknown\",\"low\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"unknown\",\"unknown\",\"unknown\",\"low\"]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-122.5237734,37.9753973],[-122.5264995,37.9709171]],\"alternatives\":true,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"pk.eyJ1Ijoic2V0aC1ib3VyZ2V0IiwiYSI6ImNrYjAwNnk5ODAzYnEycnBvMTgzajdhanUifQ.vfwMqIW8sThk0s58JyvaJg\",\"uuid\":\"ckd9ao6hl13a97ars2byaymo7\"},\"voiceLocale\":\"en-US\"}"


    class CustomPuckDrawableSupplier : PuckDrawableSupplier {
        override fun getPuckDrawable(routeProgressState: RouteProgressState): Int =
            when (routeProgressState) {
                RouteProgressState.ROUTE_INVALID -> R.drawable.custom_puck_icon_uncertain_location
                RouteProgressState.ROUTE_INITIALIZED -> R.drawable.custom_user_puck_icon
                RouteProgressState.LOCATION_TRACKING -> R.drawable.custom_user_puck_icon
                RouteProgressState.ROUTE_COMPLETE -> R.drawable.custom_puck_icon_uncertain_location
                RouteProgressState.LOCATION_STALE -> R.drawable.custom_user_puck_icon
                else -> R.drawable.custom_puck_icon_uncertain_location
            }
    }

}
