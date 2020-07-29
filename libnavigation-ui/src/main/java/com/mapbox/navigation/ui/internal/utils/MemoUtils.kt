package com.mapbox.navigation.ui.internal.utils

import java.util.concurrent.ConcurrentHashMap

private interface MemoizeCall<in F, out R> {
    operator fun invoke(func: F): R
}

private class MemoizeHandler<F, in K : MemoizeCall<F, R>, out R>(val func: F) {
    private val callMap = ConcurrentHashMap<K, R>()
    operator fun invoke(key: K): R {
        return callMap[key] ?: run {
            val result = key(func)
            callMap.putIfAbsent(key, result)
            result
        }
    }
}

private data class MemoizeKey1<out P1, R>(val p1: P1) : MemoizeCall<(P1) -> R, R> {
    override fun invoke(func: (P1) -> R) = func(p1)
}

private data class MemoizeKey3<out P1, out P2, out P3, R>(val p1: P1, val p2: P2, val p3: P3) : MemoizeCall<(P1, P2, P3) -> R, R> {
    override fun invoke(func: (P1, P2, P3) -> R) = func(p1, p2, p3)
}

fun <P1, R> ((P1) -> R).memoize(): (P1) -> R {
    return object : (P1) -> R {
        private val handler = MemoizeHandler<((P1) -> R), MemoizeKey1<P1, R>, R>(this@memoize)
        override fun invoke(p1: P1) = handler(MemoizeKey1(p1))
    }
}

fun <P1, P2, P3, R> ((P1, P2, P3) -> R).memoize(): (P1, P2, P3) -> R {
    return object : (P1, P2, P3) -> R {
        private val handler = MemoizeHandler<((P1, P2, P3) -> R), MemoizeKey3<P1, P2, P3, R>, R>(this@memoize)
        override fun invoke(p1: P1, p2: P2, p3: P3) = handler(MemoizeKey3(p1, p2, p3))
    }
}