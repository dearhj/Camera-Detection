package com.android.detection.detection

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.android.detection.detection.util.PermissionUtils

@RequiresApi(Build.VERSION_CODES.R)
class FirstActivity : AppCompatActivity() {
    private val requestCodeCamera: Int = 0x86
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        setContentView(R.layout.activity_first)
        requestFilePermission()


        if (PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            PermissionUtils.requestPermission(
                this, Manifest.permission.CAMERA, requestCodeCamera
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (App.isOut) finish()
    }

    fun requestFilePermission() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:${applicationContext.packageName}".toUri()
            startActivityForResult(intent, 2)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (Environment.isExternalStorageManager()) {
            } else {
                finish()
                Toast.makeText(this, "文件读写权限被拒绝!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCamera)
            requestCameraPermissionResult(permissions, grantResults)
    }

    /**
     * 请求Camera权限回调结果
     *
     * @param permissions  权限
     * @param grantResults 授权结果
     */
    fun requestCameraPermissionResult(permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.requestPermissionsResult(
                Manifest.permission.CAMERA, permissions, grantResults
            )
        ) startActivity(Intent(this, MainActivity::class.java))
        else finish()
    }
}