package com.mapbox.navigation.ui.internal.utils

import org.junit.Assert.*
import org.junit.Test

class MemoizeUtilsTest {

    @Test
    fun memoizeKey1Test() {
        val delay = 100L
        val addOneFun: (i: Int) -> Int = { i: Int ->
            Thread.sleep(delay)
            i + 1
        }.memoize()

        val firstStartTime = System.currentTimeMillis()
        val firstResult = addOneFun(1)
        val firstRunTime = System.currentTimeMillis() - firstStartTime

        val secondStartTime = System.currentTimeMillis()
        val secondResult = addOneFun(1)
        val secondRunTime = System.currentTimeMillis() - secondStartTime

        assertTrue(secondRunTime + (delay / 2) < firstRunTime)
        assertEquals(firstResult, secondResult)
    }

    @Test
    fun memoizeKey3Test() {
        val delay = 100L
        val sumFun: (a: Int, b: Int, c: Int) -> Int = { a: Int, b: Int, c: Int ->
            Thread.sleep(delay)
            a + b + c
        }.memoize()

        val firstStartTime = System.currentTimeMillis()
        val firstResult = sumFun(1, 2, 3)
        val firstRunTime = System.currentTimeMillis() - firstStartTime

        val secondStartTime = System.currentTimeMillis()
        val secondResult = sumFun(1, 2, 3)
        val secondRunTime = System.currentTimeMillis() - secondStartTime

        assertTrue(secondRunTime + (delay / 2) < firstRunTime)
        assertEquals(firstResult, secondResult)
    }
}