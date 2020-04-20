package com.mapbox.navigation.ui.alert

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders

class MapboxAlertView(private val context: Context) {

    private val viewModel by lazy(mode = LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(context as FragmentActivity).get(AlertViewModel::class.java)
    }

    fun showAlertView(text: String) {
        viewModel.showAlertView(text)
    }

    fun hideView() {

    }
}