package com.joe.camerautils.utils

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import java.lang.Long.signum

/**
 * =====================
 * Auther joe
 * Date---19/06/06
 */
class CompareSizesByArea : Comparator<Size> {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun compare(o1: Size?, o2: Size?): Int {
        return signum(o1!!.width.toLong()* o1.height - o2!!.width.toLong() * o2.height)
    }
}