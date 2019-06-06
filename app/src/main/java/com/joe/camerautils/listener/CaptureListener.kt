package com.joe.camerautils.listener

interface CaptureListener {
    fun caputre()
    fun recorderShort()
    fun recorderStart()
    fun recorderEnd(recorder_time:Long)
    fun recorderZoom()
    fun error(error:String)
}