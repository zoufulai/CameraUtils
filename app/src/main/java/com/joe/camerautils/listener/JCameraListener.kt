package com.joe.camerautils.listener

interface JCameraListener {
    fun caputre()
    fun recorderShort()
    fun recorderStart()
    fun recorderEnd(time: Long)
    fun cancel()
    fun quit()
}