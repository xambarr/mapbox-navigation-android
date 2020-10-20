package com.mapbox.navigation.ui.routealert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk

class MapboxRouteAlertsDisplayOptionsTest :
    BuilderTest<MapboxRouteAlertsDisplayOptions, MapboxRouteAlertsDisplayOptions.Builder>() {
    override fun getImplementationClass() = MapboxRouteAlertsDisplayOptions::class

    override fun getFilledUpBuilder() = MapboxRouteAlertsDisplayOptions.Builder(
        mockk(),
        mockk()
    ).apply {
        showToll(true)
    }

    override fun trigger() {
        // see docs
    }
}
