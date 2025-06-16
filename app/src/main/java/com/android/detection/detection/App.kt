package com.android.detection.detection

import android.app.Application

class App : Application() {
    companion object {
        var isOut = false
    }

    override fun onCreate() {
        super.onCreate()
        isOut = false
    }
}