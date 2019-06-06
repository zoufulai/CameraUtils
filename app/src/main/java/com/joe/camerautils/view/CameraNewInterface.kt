package com.cjt2325.kotlin_jcameraview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.NonNull
import android.support.annotation.RequiresApi
import android.util.Log
import android.util.Size
import android.view.Surface
import com.cjt2325.kotlin_jcameraview.util.i
import com.joe.camerautils.utils.CompareSizesByArea
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_AUTO
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_AWLAYS_OPEN
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_CLOSE
import com.joe.camerautils.utils.ConstUtil.FLASH_TYPE_OPEN
import com.joe.camerautils.utils.ImageSaver
import com.joe.camerautils.view.AutoFitTextureView
import java.io.File
import java.util.*


/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/8/10
 * 描    述：
 * =====================================
 */
class CameraNewInterface private constructor() {

    companion object {
        const val PREVIEW_WIDTH = 720                                         //预览的宽度
        const val PREVIEW_HEIGHT = 1280                                       //预览的高度
        const val SAVE_WIDTH = 720                                            //保存图片的宽度
        const val SAVE_HEIGHT = 1280                                          //保存图片的高度

        fun getInstance(): CameraNewInterface {
            return Inner.cameraNewInterface
        }
    }

    private var mCameraId: String? = null
    private lateinit var mCameraCharacteristics: CameraCharacteristics
    private var mTextureView: AutoFitTextureView? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null

    private var mCameraManager: CameraManager? = null
    private var mImageReader: ImageReader? = null
    private var mBackgroundHandler: Handler? = null
    private val handlerThread = HandlerThread("CameraThread")
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    init {
        handlerThread.start()
        mBackgroundHandler = Handler(handlerThread.looper)
    }

    private object Inner {
        var cameraNewInterface = CameraNewInterface()
    }

    private var preview_width: Int = 0
    private var preview_height: Int = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)                      //预览大小
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mSavePicSize = Size(SAVE_WIDTH, SAVE_HEIGHT)                            //保存图片大小

    private var mCameraSensorOrientation = 0                                            //摄像头方向

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun openCamera(context: Context, textureView: AutoFitTextureView, width: Int, height: Int) {
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mTextureView = textureView

        val cameraIdList = mCameraManager!!.cameraIdList
        if (cameraIdList.isEmpty()) {
            //mActivity.toast("没有可用相机")
            return
        }

        for (id in cameraIdList) {
            val cameraCharacteristics = mCameraManager!!.getCameraCharacteristics(id)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

            if (facing == mCameraFacing) {
                mCameraId = id
                mCameraCharacteristics = cameraCharacteristics
            }
            //log("设备中的摄像头 $id")
        }

        preview_width = width
        preview_height = height

        val supportLevel = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
//            mActivity.toast("相机硬件不支持新特性")
        }

        //获取摄像头方向
        mCameraSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
        val configurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val savePicSize = configurationMap.getOutputSizes(ImageFormat.JPEG)          //保存照片尺寸
        val previewSize = configurationMap.getOutputSizes(SurfaceTexture::class.java) //预览尺寸

        context as Activity
        var mDisplayRotation = context.windowManager.defaultDisplay.orientation
        val exchange = exchangeWidthAndHeight(mDisplayRotation, mCameraSensorOrientation)

        mSavePicSize = getBestSize(
            if (exchange) mSavePicSize.height else mSavePicSize.width,
            if (exchange) mSavePicSize.width else mSavePicSize.height,
            if (exchange) mSavePicSize.height else mSavePicSize.width,
            if (exchange) mSavePicSize.width else mSavePicSize.height,
            savePicSize.toList()
        )

        mPreviewSize = getBestSize(
            if (exchange) mPreviewSize!!.height else mPreviewSize!!.width,
            if (exchange) mPreviewSize!!.width else mPreviewSize!!.height,
            if (exchange) textureView.height else textureView.width,
            if (exchange) textureView.width else textureView.height,
            previewSize.toList()
        )

        textureView.surfaceTexture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
        mImageReader = ImageReader.newInstance(mSavePicSize.width, mSavePicSize.height, ImageFormat.JPEG, 1)
        mImageReader?.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)

        mCameraManager?.openCamera(mCameraId, mStateCallback, null)

    }

    /**
     *
     * 根据提供的参数值返回与指定宽高相等或最接近的尺寸
     *
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @param maxWidth      最大宽度(即TextureView的宽度)
     * @param maxHeight     最大高度(即TextureView的高度)
     * @param sizeList      支持的Size列表
     *
     * @return  返回与指定宽高相等或最接近的尺寸
     *
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getBestSize(
        targetWidth: Int,
        targetHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        sizeList: List<Size>
    ): Size {
        val bigEnough = ArrayList<Size>()     //比指定宽高大的Size列表
        val notBigEnough = ArrayList<Size>()  //比指定宽高小的Size列表

        for (size in sizeList) {

            //宽<=最大宽度  &&  高<=最大高度  &&  宽高比 == 目标值宽高比
            if (size.width <= maxWidth && size.height <= maxHeight
                && size.width == size.height * targetWidth / targetHeight
            ) {

                if (size.width >= targetWidth && size.height >= targetHeight)
                    bigEnough.add(size)
                else
                    notBigEnough.add(size)
            }
            //log("系统支持的尺寸: ${size.width} * ${size.height} ,  比例 ：${size.width.toFloat() / size.height}")
        }

        //log("最大尺寸 ：$maxWidth * $maxHeight, 比例 ：${targetWidth.toFloat() / targetHeight}")
        //log("目标尺寸 ：$targetWidth * $targetHeight, 比例 ：${targetWidth.toFloat() / targetHeight}")

        //选择bigEnough中最小的值  或 notBigEnough中最大的值
        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> sizeList[0]
        }
    }

    /**
     * 根据提供的屏幕方向 [displayRotation] 和相机方向 [sensorOrientation] 返回是否需要交换宽高
     */
    private fun exchangeWidthAndHeight(displayRotation: Int, sensorOrientation: Int): Boolean {
        var exchange = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    exchange = true
                }
            Surface.ROTATION_90, Surface.ROTATION_270 ->
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    exchange = true
                }
            else -> Log.e("Error", "Display rotation is invalid: $displayRotation")
        }

//        log("屏幕方向  $displayRotation")
//        log("相机方向  $sensorOrientation")
        return exchange
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopCamera() {
        if (null == mCameraDevice)
            return
        mCameraDevice?.close()
    }

    private val mCameraCaptureSessionCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.StateCallback() {

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onConfigured(session: CameraCaptureSession?) {
            if (null == mCameraDevice) {
                i("null == mCameraDevice")
                return
            }
            // 当摄像头已经准备好时，开始显示预览
            mCaptureSession = session
            // 自动对焦
            mPreviewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            // 打开闪光灯
            when (flashType) {
                FLASH_TYPE_AUTO -> {
                    Log.e("Error", "0000000000auto000000000")
                    Log.e("Error", "0000000000close000000000")
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF
                    )
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                }
                FLASH_TYPE_OPEN -> {
                    Log.e("Error", "0000000000open000000000")
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    )
                }
                FLASH_TYPE_CLOSE -> {
                    Log.e("Error", "0000000000close000000000")
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    )
                }
                FLASH_TYPE_AWLAYS_OPEN -> {
                    Log.e("Error", "0000000000close000000000")
                    mPreviewRequestBuilder!!.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH
                    )
                }
            }
            // 显示预览

            if (mPreviewRequestBuilder == null) {
                i("mPreviewRequestBuilder ==null")
            } else {
                var previewRequest: CaptureRequest = mPreviewRequestBuilder?.build() as CaptureRequest
                mCaptureSession?.setRepeatingRequest(previewRequest, object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        canExchangeCamera = true
                        canTakePic = true
                    }
                }, null)
                i("启动浏览？？？？？？？？？？？")
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession?) {
            i("onConfigureFailed")
        }

    }

    //打开相机时候的监听器，通过他可以得到相机实例，这个实例可以创建请求建造者
    private val mStateCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onOpened(cameraDevice: CameraDevice) {
            this@CameraNewInterface.mCameraDevice = cameraDevice
            i("相机已经打开")
            takePreview()
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            i("相机连接断开")
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onError(cameraDevice: CameraDevice, i: Int) {
            cameraDevice.close()
            this@CameraNewInterface.mCameraDevice = null
            i("相机打开失败")
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun takePreview() {
        // 创建预览需要的CaptureRequest.Builder
        mPreviewRequestBuilder =
            mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW) as CaptureRequest.Builder
        // 将SurfaceView的surface作为CaptureRequest.Builder的目标
        var sufaceTexture: SurfaceTexture = mTextureView?.surfaceTexture as SurfaceTexture
        sufaceTexture.setDefaultBufferSize(1280, 720)
        var surface: Surface = Surface(sufaceTexture)
        mPreviewRequestBuilder?.addTarget(surface)
        // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求

        mCameraDevice?.createCaptureSession(
            Arrays.asList(surface, mImageReader?.surface),
            mCameraCaptureSessionCallback,
            null
        )
    }


    var cameraHandler: Handler? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun takePicture() {
        if (mCameraDevice == null || !mTextureView!!.isAvailable || !canTakePic) return
        lockFocus()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lockFocus() {
        i("lockFocus")
        // This is how to tell the camera to lock focus.
        mPreviewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        // Tell #mCaptureCallback to wait for the lock.
        mState = STATE_WAITING_LOCK
        mCaptureSession?.capture(mPreviewRequestBuilder?.build(), mCaptureCallback, mBackgroundHandler)
    }

    private val STATE_PREVIEW = 0

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera state: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_WIDTH = 1920

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_HEIGHT = 1080

    var mState: Int? = null

    private val mCaptureCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.CaptureCallback() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun process(result: CaptureResult) {
            i("process")
            when (mState) {
                STATE_PREVIEW -> {
                }// We have nothing to do when the camera preview is working normally.
                STATE_WAITING_LOCK -> {
                    i("STATE_WAITING_LOCK")
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == null) {
                        captureStillPicture()
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        || CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN == afState
                        || CaptureRequest.CONTROL_AF_STATE_INACTIVE == afState
                    ) {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState === CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        } else {
                            runPrecaptureSequence()
                        }
                    }
                }
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState === CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState === CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        mState = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState !== CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        override fun onCaptureProgressed(
            @NonNull session: CameraCaptureSession,
            @NonNull request: CaptureRequest,
            @NonNull partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            @NonNull session: CameraCaptureSession,
            @NonNull request: CaptureRequest,
            @NonNull result: TotalCaptureResult
        ) {
            process(result)
        }

    }


    /**
     * 具体拍照操作
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun captureStillPicture() {
        if (null == mCameraDevice) {
            return
        }
        // This is the CaptureRequest.Builder that we use to take a picture.
        val captureBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder?.addTarget(mImageReader?.surface)

        // Use the same AE and AF modes as the preview.
        captureBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        if (captureBuilder != null) {
//            mPreviewRequestBuilder!!.set(
//                CaptureRequest.CONTROL_AE_MODE,
//                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//            )
        }

        captureBuilder!!.set(
            CaptureRequest.JPEG_ORIENTATION,
            mCameraSensorOrientation
        )      //根据摄像头方向对保存的照片进行旋转，使其为"自然方向"

        val CaptureCallback = object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
//                showToast("Saved: " + mFile)
//                Log.d(TAG, mFile.toString())
                unlockFocus()
            }
        }

        mCaptureSession?.stopRepeating()
        mCaptureSession?.capture(captureBuilder?.build(), CaptureCallback, null)
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            //setAutoFlash(mPreviewRequestBuilder!!)
            mCaptureSession?.capture(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )

            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW
            var previewRequest: CaptureRequest = mPreviewRequestBuilder?.build() as CaptureRequest
            mCaptureSession?.setRepeatingRequest(
                previewRequest, mCaptureCallback,
                mBackgroundHandler
            )

        } catch (e: CameraAccessException) {
            //Log.e(TAG, e.toString())
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun runPrecaptureSequence() {
        // This is how to tell the camera to trigger.
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
        )
        // Tell #mCaptureCallback to wait for the precapture sequence to be set.
        mState = STATE_WAITING_PRECAPTURE
        mCaptureSession?.capture(
            mPreviewRequestBuilder?.build(), mCaptureCallback,
            mBackgroundHandler
        )
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        i("takePicture Success " + reader.height)
        mFile = File(path + "/" + System.currentTimeMillis() + ".png")
        if (!mFile!!.exists())
            mFile!!.createNewFile()
        mFile?.let {
            mBackgroundHandler!!.post(
                ImageSaver(reader.acquireNextImage(), it, cameraHandler!!, mCameraSensorOrientation == 270)
            )
        }
    }


    private var canExchangeCamera = false//是否可以切换摄像头
    private var canTakePic = false //是否可以拍照
    private var mCameraFacing = CameraCharacteristics.LENS_FACING_BACK             //默认使用后置摄像头

    /**
     * 切换摄像头
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun exchangeCamera(context: Context, textureView: AutoFitTextureView, width: Int, height: Int) {
        if (mCameraDevice == null || !canExchangeCamera || !mTextureView!!.isAvailable) return

        mCameraFacing = if (mCameraFacing == CameraCharacteristics.LENS_FACING_FRONT)
            CameraCharacteristics.LENS_FACING_BACK
        else
            CameraCharacteristics.LENS_FACING_FRONT
        releaseCamera()
        openCamera(context, textureView, width, height)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun releaseCamera() {
        mCaptureSession?.close()
        mCaptureSession = null

        mCameraDevice?.close()
        mCameraDevice = null

        mImageReader?.close()
        mImageReader = null

        canExchangeCamera = false
    }

    private var mFile: File? = null
    private lateinit var path: String
    fun setCachePath(path: String) {
        this.path = path
    }

    private var flashType: Int = FLASH_TYPE_AUTO
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setFlashType(flashType: Int) {
        this.flashType = flashType

        when (flashType) {
            FLASH_TYPE_AUTO -> {
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
                Log.e("Error", "0000000000auto000000000")
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }
            FLASH_TYPE_OPEN -> {
                Log.e("Error", "0000000000open000000000")
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
            }
            FLASH_TYPE_CLOSE -> {
                Log.e("Error", "0000000000close000000000")
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
            }
            FLASH_TYPE_AWLAYS_OPEN -> {
                Log.e("Error", "0000000000close000000000")
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH
                )
            }
        }
        mCaptureSession!!.setRepeatingRequest(mPreviewRequestBuilder!!.build(), mCaptureCallback, mBackgroundHandler)
    }

    fun getFile(): File? {
        return mFile
    }
}