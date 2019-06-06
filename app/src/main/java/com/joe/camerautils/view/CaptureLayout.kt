package com.joe.camerautils.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.joe.camerautils.listener.CaptureListener
import com.joe.camerautils.listener.QuitListener
import com.joe.camerautils.listener.TypeListener
import com.joe.camerautils.utils.getScreenWidth

class CaptureLayout(context: Context) : FrameLayout(context) {
    private var layout_width: Int
    private var layout_height: Int
    private var button_size: Float

    private var captureButton: CaptureButton
    private var cancleButton: TypeButton
    private var confirmButton: TypeButton
    private var quitButton: QuitButton
    private var textView_tip: TextView

    var mCaptureListener: CaptureListener? = null
    var mQuitListener: QuitListener? = null
    var mTypeListener: TypeListener? = null

    init {
        layout_width = getScreenWidth(context)
        button_size = layout_width / 4.5f //按钮大小
        layout_height = (button_size + (button_size / 5) * 2 + 100).toInt()

        //拍照按钮部分（CaptureButton）
        captureButton = CaptureButton(context, button_size)
        val capture_param = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        capture_param.gravity = Gravity.CENTER
        captureButton.layoutParams = capture_param

        //取消按钮(TypeButton)
        cancleButton = TypeButton(context, TypeButton.Companion.TYPE_CANCEL, button_size)
        val cancle_param =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        cancle_param.gravity = Gravity.CENTER_VERTICAL
        cancle_param.setMargins(layout_width / 6, 0, 0, 0)
        cancleButton.layoutParams = cancle_param
//
        //确定按钮(TypeButton)
        confirmButton = TypeButton(context, TypeButton.Companion.TYPE_CONFIRM, button_size)
        val confirm_param =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        confirm_param.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        confirm_param.setMargins(0, 0, layout_width / 6, 0)
        confirmButton.layoutParams = confirm_param

        //取消按钮（TypeButton）
        quitButton = QuitButton(context, (button_size * 0.4).toInt())
        val quit_param =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        quit_param.gravity = Gravity.CENTER_VERTICAL
        quit_param.setMargins(layout_width / 6, 0, 0, 0)
        quitButton.layoutParams = quit_param


        //内容提示
        textView_tip = TextView(context)
        textView_tip.setTextColor(0xFFFFFFFF.toInt())
        textView_tip.text = "Kotlin JCamera"
        val tip_param =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
        tip_param.gravity = Gravity.CENTER_HORIZONTAL
        textView_tip.layoutParams = tip_param

        this.addView(captureButton)
        this.addView(quitButton)
        this.addView(cancleButton)
        this.addView(confirmButton)
        this.addView(textView_tip)

        cancleButton.visibility = View.INVISIBLE
        confirmButton.visibility = View.INVISIBLE

        initListenter()
    }

    private fun initListenter() {
        cancleButton.setOnClickListener {
            resetLayout()
            if (mTypeListener != null) {
                mTypeListener?.cancel()
            }
        }

        confirmButton.setOnClickListener {
            resetLayout()
            if (mTypeListener != null)
                mTypeListener?.confirm()
        }

        quitButton.setOnClickListener {
            if (mQuitListener != null)
                mQuitListener?.quit()
        }

        captureButton.mCaptureListener = object : CaptureListener {

            override fun error(error: String) {
            }

            override fun recorderZoom() {
            }


            override fun recorderEnd(time: Long) {
                if (mCaptureListener != null)
                    mCaptureListener?.recorderEnd(time)
            }

            override fun recorderShort() {
                if (mCaptureListener != null)
                    mCaptureListener?.recorderShort()
            }

            override fun caputre() {
                if (mCaptureListener != null) {
                    mCaptureListener?.caputre()
                }
            }

            override fun recorderStart() {
                if (mCaptureListener != null)
                    mCaptureListener?.recorderStart()
            }
        }
    }

    fun startButtonAnimation() {
        cancleButton.visibility = View.VISIBLE
        confirmButton.visibility = View.VISIBLE
        captureButton.visibility = View.INVISIBLE
        quitButton.visibility = View.VISIBLE
        //拍照
        cancleButton.isClickable = false
        confirmButton.isClickable = false
        val animator_cancel = ObjectAnimator.ofFloat(cancleButton, "translationX", layout_width / 4f, 0f)
        animator_cancel.duration = 200
        animator_cancel.start()

        val animator_confirm = ObjectAnimator.ofFloat(confirmButton, "translationX", -layout_width / 4f, 0f)
        animator_confirm.duration = 200
        animator_confirm.start()
        animator_confirm.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                cancleButton.isClickable = true
                confirmButton.isClickable = true
            }
        })
    }

    fun resetLayout() {
        cancleButton.visibility = View.INVISIBLE
        confirmButton.visibility = View.INVISIBLE
        captureButton.visibility = View.VISIBLE
        quitButton.visibility = View.VISIBLE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(layout_width, layout_height)
    }
}