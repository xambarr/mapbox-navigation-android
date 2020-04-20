package com.mapbox.navigation.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

@SuppressLint("Recycle")
fun Context.getStyledAttributes(
    attributeSet: AttributeSet?,
    styledArray: IntArray,
    block: TypedArray.() -> Unit
) = this.obtainStyledAttributes(attributeSet, styledArray).use(block)