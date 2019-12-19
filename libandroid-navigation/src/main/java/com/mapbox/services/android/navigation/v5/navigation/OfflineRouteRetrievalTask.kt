package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.google.gson.Gson
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouterResult

internal class OfflineRouteRetrievalTask(
    private val navigator: Navigator,
    private val callback: OnOfflineRouteFoundCallback
) : AsyncTask<OfflineRoute, Void, DirectionsRoute>() {

    @Volatile private lateinit var routerResult: RouterResult

    // For testing only
    internal constructor(
        navigator: Navigator,
        callback: OnOfflineRouteFoundCallback,
        routerResult: RouterResult
    ) : this(navigator, callback) {
        this.routerResult = routerResult
    }

    companion object {
        private const val FIRST_ROUTE = 0
    }

    override fun doInBackground(vararg offlineRoutes: OfflineRoute): DirectionsRoute? {
        val url = offlineRoutes[FIRST_ROUTE].buildUrl()

        synchronized(navigator) {
            routerResult = navigator.getRoute(url)
        }

        return offlineRoutes[FIRST_ROUTE].retrieveOfflineRoute(routerResult)
    }

    public override fun onPostExecute(offlineRoute: DirectionsRoute?) {
        if (offlineRoute != null) {
            callback.onRouteFound(offlineRoute)
        } else {
            callback.onError(OfflineError(generateErrorMessage()))
        }
    }

    private fun generateErrorMessage(): String {
        val (_, _, error, errorCode) = Gson().fromJson(routerResult.json, OfflineRouteError::class.java)

        val errorMessage = "Error occurred fetching offline route: $error - Code: $errorCode"

        MapboxLogger.e(Message(errorMessage))
        return errorMessage
    }
}