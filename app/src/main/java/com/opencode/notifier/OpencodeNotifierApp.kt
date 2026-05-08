package com.opencode.notifier

import android.app.Application

class OpencodeNotifierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.i("APP", "App started")
    }
}
