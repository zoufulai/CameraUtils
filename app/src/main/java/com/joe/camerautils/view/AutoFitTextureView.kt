package com.joe.camerautils.view

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import java.lang.IllegalArgumentException

/**
 * =====================
 * Auther joe
 * Date---19/06/06
 */
class AutoFitTextureView constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    TextureView(context, attrs, defStyle) {
    private var mRatioWidth = 0
    private var mRatioHeight = 0

    fun setAspectRatio(width:Int,height:Int){
        if(width<0||height<0){
            throw  IllegalArgumentException("Size cannot be negative.")
        }
        mRatioHeight = height
        mRatioWidth = width
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        if(0==mRatioWidth||0==mRatioHeight){
            setMeasuredDimension(width,height)
        }else{
            if(width<height*mRatioWidth/mRatioHeight){
                setMeasuredDimension(width,width*mRatioHeight/mRatioWidth)
            }else{
                setMeasuredDimension(height*mRatioWidth/mRatioHeight,height)
            }
        }
    }

}