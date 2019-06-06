package com.joe.camerautils.listener

/**
 * =====================
 * Auther joe
 * Date---19/06/06
 */
interface JCameraListener {
    fun caputre()
    fun recorderShort()
    fun recorderStart()
    fun recorderEnd(time: Long)
    fun cancel()
    fun quit()
}