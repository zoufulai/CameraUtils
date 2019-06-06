package com.joe.camerautils.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.cjt2325.kotlin_jcameraview.CameraNewInterface
import com.joe.camerautils.listener.CaptureListener
import com.joe.camerautils.listener.QuitListener
import com.joe.camerautils.listener.TypeListener
import com.joe.camerautils.utils.ConstUtil.FILE_COPY_SAVE
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_AUTO
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_AWLAYS_OPEN
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_CLOSE
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_OPEN
import com.joe.camerautils.utils.ConstUtil.IMG_PREVIEW_HIDE
import com.joe.camerautils.utils.ConstUtil.IMG_PREVIEW_SHOW
import com.joe.camerautils.utils.ConstUtil.VEDIO_PREVIEW_HIDE
import com.joe.camerautils.utils.ConstUtil.VEDIO_PREVIEW_SHOW
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class CameraView : FrameLayout, TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        //设置图片缓存路径
        CameraNewInterface.getInstance(context).cameraHandler = cameraHandler
        CameraNewInterface.getInstance(context).openCamera(textureView, width, height)
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onResume() {
        CameraNewInterface.getInstance(context).startBackgroundThread()
        if (textureView.isAvailable) {
            CameraNewInterface.getInstance(context).openCamera(textureView, width, height)
        } else {
            textureView.surfaceTextureListener = this
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onPause() {
        CameraNewInterface.getInstance(context).closeCamera()
        CameraNewInterface.getInstance(context).stopBackgroundThread()
    }

    var textureView: AutoFitTextureView
    var captureLayout: CaptureLayout
    var switchCamera: ImageView
    var switchFlash: ImageView
    var previewImg: ImageView

    var flashType: Int = FLASH_TYPE_AUTO

    init {
        textureView = AutoFitTextureView(context)
        captureLayout = CaptureLayout(context)
        switchCamera = ImageView(context)
        switchFlash = ImageView(context)
        previewImg = ImageView(context)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    constructor(context: Context) : this(context, null)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initAttr(attributeSet)
        initView()
        initListener()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attributeSet,
        defStyleAttr,
        defStyleRes
    ) {
        initAttr(attributeSet)
        initView()
        initListener()
    }

    fun initAttr(attributeSet: AttributeSet?) {

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun initView() {
        //CaptureLayout
        val captureLayout_param =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        captureLayout_param.gravity = Gravity.BOTTOM
        captureLayout.layoutParams = captureLayout_param

        //TextureView
        val textureView_param =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        textureView.layoutParams = textureView_param
        textureView.surfaceTextureListener = this

        val switchcamera_param = LayoutParams(60, 60)
        switchcamera_param.gravity = Gravity.RIGHT
        switchcamera_param.setMargins(16, 16, 16, 16)
        switchCamera.layoutParams = switchcamera_param
        switchCamera.setImageResource(com.joe.camerautils.R.drawable.ic_camera)
        switchCamera.setOnClickListener {
            CameraNewInterface.getInstance(context).exchangeCamera(textureView, width, height)
        }

        val switchflash_param = LayoutParams(60, 60)
        switchflash_param.gravity = Gravity.RIGHT
        switchflash_param.setMargins(16, 16, 48 + 60, 16)
        switchFlash.layoutParams = switchflash_param
        switchFlash.setImageResource(com.joe.camerautils.R.drawable.ic_l1)
        switchFlash.setOnClickListener {
            when (flashType) {
                FLASH_TYPE_AUTO -> {
                    flashType = FLASH_TYPE_OPEN
                    switchFlash.setImageResource(com.joe.camerautils.R.drawable.ic_l2)
                    CameraNewInterface.getInstance(context).setFlashType(flashType)
                }
                FLASH_TYPE_OPEN -> {
                    flashType = FLASH_TYPE_CLOSE
                    switchFlash.setImageResource(com.joe.camerautils.R.drawable.ic_l3)
                    CameraNewInterface.getInstance(context).setFlashType(flashType)
                }
                FLASH_TYPE_CLOSE -> {
                    flashType = FLASH_TYPE_AWLAYS_OPEN
                    switchFlash.setImageResource(com.joe.camerautils.R.drawable.ic_l4)
                    CameraNewInterface.getInstance(context).setFlashType(flashType)
                }
                FLASH_TYPE_AWLAYS_OPEN -> {
                    flashType = FLASH_TYPE_AUTO
                    switchFlash.setImageResource(com.joe.camerautils.R.drawable.ic_l1)
                    CameraNewInterface.getInstance(context).setFlashType(flashType)
                }
            }
        }

        val previewimg_param = LayoutParams(600, 600)
        previewimg_param.gravity = Gravity.LEFT
        previewimg_param.setMargins(16, 5, 0, 0)
        previewImg.layoutParams = previewimg_param
        previewImg.visibility = View.GONE

        this.addView(textureView)
        this.addView(previewImg)
        this.addView(captureLayout)
        this.addView(switchCamera)
        this.addView(switchFlash)
    }

    fun initListener() {
        captureLayout.mQuitListener = object : QuitListener {
            override fun quit() {
                operationListener.quite()
            }
        }
        captureLayout.mTypeListener = object : TypeListener {
            override fun cancel() {
                var msg = cameraHandler.obtainMessage()
                msg.what = IMG_PREVIEW_HIDE
                cameraHandler.sendMessage(msg)
                operationListener.cancel()
            }

            override fun confirm() {
                //在子线程处理耗时操作
                Thread(Runnable {
                    var file = File(filePath)
                    //copy文件保存
                    var bytesum = 0
                    if (file != null && file!!.exists()) { //文件存在时
                        Log.e("Error", "00000000000=====" + file.path)
                        var of = File(savePath + "/" + file.name)
                        if (!of.exists())
                            of.createNewFile()
                        val inStream = FileInputStream(file) //读入原文件
                        val fs = FileOutputStream(of)
                        val buffer = ByteArray(1024)
                        while (true) {
                            var byteread = inStream.read(buffer)
                            if (byteread != -1) {
                                bytesum += byteread //字节数 文件大小
                                fs.write(buffer, 0, byteread)
                            } else {
                                break
                            }
                        }
                        inStream.close()

                        var msg = cameraHandler.obtainMessage()
                        msg.what = FILE_COPY_SAVE
                        var data = Bundle()
                        data.putString("path", of.path)
                        msg.data = data
                        cameraHandler.sendMessage(msg)
                    }
                }).start()
            }

        }
        captureLayout.mCaptureListener = object : CaptureListener {
            override fun error(error: String) {
            }

            override fun recorderZoom() {

            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun caputre() {
                CameraNewInterface.getInstance(context).takePicture()
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun recorderEnd(time: Long) {
                CameraNewInterface.getInstance(context).stopRecordingVideo()
            }

            override fun recorderShort() {

            }

            //开始录制视频
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun recorderStart() {
                CameraNewInterface.getInstance(context).startRecordingVideo()
            }
        }
    }


    /**

     * 获取视频文件截图

     *

     * @param path 视频文件的路径

     * @return Bitmap 返回获取的Bitmap

     */

    private fun getVideoThumb(path: String): Bitmap {

        val media = MediaMetadataRetriever()

        media.setDataSource(path)

        return media.getFrameAtTime()

    }

    private var filePath: String = ""

    private val cameraHandler = object : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg!!.what) {
                IMG_PREVIEW_SHOW -> {
                    var path = msg.data.getString("path")
                    previewImg.setImageBitmap(BitmapFactory.decodeFile(path))
                    filePath = path
                    previewImg.visibility = View.VISIBLE
                    captureLayout.startButtonAnimation()
                }
                IMG_PREVIEW_HIDE -> {
                    previewImg.visibility = View.GONE

                    //删除缓存文件
                    previewImg.setImageBitmap(null)
                    File(filePath).delete()
                }
                FILE_COPY_SAVE -> {
                    //返回
                    var path = msg.data.getString("path")
                    operationListener.save(File(path))

                    filePath = ""
                    previewImg.visibility = View.GONE
                }
                VEDIO_PREVIEW_SHOW -> {
                    var path = msg.data.getString("path")
                    filePath = path
                    previewImg.setImageBitmap(getVideoThumb(path))
                    previewImg.visibility = View.VISIBLE
                    captureLayout.startButtonAnimation()
                }
                VEDIO_PREVIEW_HIDE -> {
                    previewImg.visibility = View.GONE
                    //删除缓存文件
                    previewImg.setImageBitmap(null)
                    File(filePath).delete()
                }
            }
        }
    }

    private var savePath: String = ""
    fun setSavePath(path: String) {
        this.savePath = path
    }

    private lateinit var operationListener: OperationListener
    fun setOperateListener(operationListener: OperationListener) {
        this.operationListener = operationListener
    }

    interface OperationListener {
        fun quite()
        fun cancel()
        fun save(file: File?)
    }

}