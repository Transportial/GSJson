package com.transportial

import java.util.*

object HelperUtils {

    fun number(digits: Int): Long {
        val str = StringBuilder()
        val random = Random()
        for (i in 0 until digits) {
            str.append(random.nextInt(10))
        }
        return str.toString().toLong()
    }
}