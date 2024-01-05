package com.r2devpros.audioplayer.core

import android.app.Application
import timber.log.Timber

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("MyApp_TAG: onCreate: ")
    }
}