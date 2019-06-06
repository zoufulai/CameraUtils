package com.joe.camerautils.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.util.Log
import com.joe.camerautils.utils.ConstUtil.IMG_PREVIEW_SHOW
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * =====================
 * Auther joe
 * Date---19/06/06
 */
class ImageSaver(
    private val mImage: Image,
    private val mFile: File,
    private val handler: Handler,
    private val isMirror: Boolean
) : Runnable {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun run() {
        Log.e("Error", "99999999999====" + Thread.currentThread().name)
        val buffer = mImage.getPlanes()[0].getBuffer()
        var bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        if (isMirror) {
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val resultBitmap = mirror(rawBitmap)
            bytes = toByteArray(resultBitmap)
        }
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(mFile)
            output!!.write(bytes)
            var msg = handler.obtainMessage()
            msg.what = IMG_PREVIEW_SHOW
            var data = Bundle()
            data.putString("path", mFile.path)
            msg.data = data
            handler.sendMessage(msg)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            mImage.close()
            if (null != output) {
                try {
                    output!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    fun mirror(rawBitmap: Bitmap): Bitmap {
        var matrix = Matrix()
        matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    fun toByteArray(bitmap: Bitmap): ByteArray {
        var os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        return os.toByteArray()
    }
}