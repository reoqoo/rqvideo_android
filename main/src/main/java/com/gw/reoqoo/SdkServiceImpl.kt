package com.gw.reoqoo

import android.app.Application
import com.gw.reoqoo.app.MainProcessApp
import com.gw.reoqoo.app.api.IReoqooSdkService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: yanzheng@gwell.cc
 * Time: 2024/10/8 20:03
 * Description: SdkServiceImpl
 */
@Singleton
class ReoqooSdkServiceImpl @Inject constructor() : IReoqooSdkService {

    companion object {
        private const val TAG = "ReoqooSdkServiceImpl"
    }

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var processApp: MainProcessApp

    override fun initSync() {
        processApp.initSync(app)
    }

    override suspend fun initService() {
        processApp.mount(app)
    }

}