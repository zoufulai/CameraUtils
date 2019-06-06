package com.joe.camerautils.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * 获取屏幕宽高
 */
fun getScreenWidth(context: Context): Int {
    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val outMetrics = DisplayMetrics()
    manager.defaultDisplay.getMetrics(outMetrics)
    return outMetrics.widthPixels
}

fun getScreenHeight(context: Context): Int {
    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val outMetrics = DisplayMetrics()
    manager.defaultDisplay.getMetrics(outMetrics)
    return outMetrics.heightPixels
}