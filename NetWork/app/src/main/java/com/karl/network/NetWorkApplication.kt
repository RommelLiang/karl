package com.karl.network

import android.app.Application
import android.content.Context
import leakcanary.AppWatcher

class NetWorkApplication : Application() {
    companion object{
        private  lateinit var context: Context

        public fun getContext():Context{
            return  context
        }
    }
    override fun onCreate() {
        super.onCreate()
        if(!AppWatcher.isInstalled) {
            AppWatcher.manualInstall(this)
        }
        context = applicationContext
    }
}