package com.joe.camerautils

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.joe.camerautils.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }


    fun initView() {
        camerview.setOperateListener(object : CameraView.OperationListener {
            override fun quite() {
                finish()
            }

            override fun cancel() {
                Log.e("Error", "00000000filepath000000-----取消")
            }

            override fun save(file: File?) {
                Log.e("Error", "00000000filepath000000" + file!!.path)
                runOnUiThread(Runnable {
                    Toast.makeText(applicationContext, "保存成功---" + file.path, Toast.LENGTH_LONG).show()
                })

            }

        })

        //生成文件夹
        var path = Environment.getExternalStorageDirectory().absolutePath + "/CameraUtils/image"
        var file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        //设置图片保存路径
        camerview.setSavePath(path)
    }
}
