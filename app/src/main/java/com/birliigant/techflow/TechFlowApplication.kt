package com.birliigant.techflow

import android.app.Application
import com.birliigant.techflow.app.AppContainer
import com.tencent.mmkv.MMKV

class TechFlowApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        appContainer = AppContainer(this)
    }
}
