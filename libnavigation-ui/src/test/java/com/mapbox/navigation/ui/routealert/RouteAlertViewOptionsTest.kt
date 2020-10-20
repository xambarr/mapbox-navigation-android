package com.mapbox.navigation.ui.routealert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class RouteAlertViewOptionsTest :
    BuilderTest<RouteAlertViewOptions, RouteAlertViewOptions.Builder>() {
    override fun getImplementationClass() = RouteAlertViewOptions::class

    override fun getFilledUpBuilder() = RouteAlertViewOptions.Builder(
        mockk(),
        mockk()
    ).apply {
        drawable(mockk())
        properties(arrayOf(mockk()))
    }

    @Test
    override fun trigger() {
        // see docs
    }
}
