package com.mapbox.navigation.ui.internal.route



/**
 * An internal Mapbox factory for creating a LayerProvider used for creating the layers needed
 * to display route line related geometry on the map.
 */
object MapboxRouteLayerProviderFactory {

    /**
     * Creates a MapboxRouteLayerProvider.
     *
     * @param routeStyleDescriptors used for programatic styling of the route lines based on a
     * route property which can be used for overriding the color styling defined in the theme.
     */
    @JvmStatic
    fun getLayerProvider() =
        object : MapboxRouteLayerProvider {}
}