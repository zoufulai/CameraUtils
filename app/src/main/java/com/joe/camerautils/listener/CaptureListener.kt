package com.joe.camerautils.listener

/**
 * =====================
 * Auther joe
 * Date---19/06/06
 */
interface CaptureListener {
    fun caputre()
    fun recorderShort()
    fun recorderStart()
    fun recorderEnd(recorder_time: Long)
    fun recorderZoom()
    fun error(error: String)
}