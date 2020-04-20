package com.mapbox.navigation.utils.extensions

import android.content.res.TypedArray

fun TypedArray.use(block : TypedArray.() -> Unit) {
    try {
        block()
    } finally {
        this.recycle()
    }
}