package com.mapbox.navigation.ui.core


import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapboxUICore private constructor(lifecycle: Lifecycle, private val context: Context): LifecycleObserver {

    constructor(frag: Fragment): this(frag.lifecycle, frag.context!!)
    constructor(act: FragmentActivity): this(act.lifecycle, act)

    private val job = SupervisorJob()
    internal val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)
    init {
        lifecycle.addObserver(this)
        observeViewUpdates()
    }
    private val viewModel: MapboxUICoreViewModel by lazy {
        ViewModelProvider(context as ViewModelStoreOwner, UICoreViewModelFactory()).get(MapboxUICoreViewModel::class.java)
    }
    private val parentActivity: Activity by lazy {
        when (context) {
            is Fragment -> context.activity as Activity
            else -> context as Activity
        }
    }
//    private val mapView: MapView by lazy {
//        parentActivity.window.findViewById(R.id.mapView)
//    }
    fun observeViewUpdates() {
        uiScope.launch {
            for(cmd in viewModel.getViewCommandUpdates()) {
                cmd(parentActivity)
            }
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun handleOnCreate() {
    }


    var counter = 0
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun handleOnResume() {
        //viewModel.resumeExample("i'm resuming.")

        uiScope.launch {
            while(true) {
                delay(500)
                viewModel.handleRouteProgress(counter)
                counter += 1
            }
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun handleOnStop() {
        job.cancel()
    }
}

class UICoreViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapboxUICoreViewModel::class.java)) {
            return MapboxUICoreViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}