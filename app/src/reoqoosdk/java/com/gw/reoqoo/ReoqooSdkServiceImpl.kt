package com.gw.reoqoo

import android.app.Application
import com.gw.reoqoo.app.api.IReoqooSdkService
import com.gw.reoqoosdk.sdk.api.IReoqooSdkMgr
import com.gw.reoqoosdk.sdk.repository.ConfigEntity
import com.gwell.loglibs.GwellLogUtils
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
    lateinit var sdkMgrImpl: IReoqooSdkMgr

    override fun initService() {
        GwellLogUtils.i(TAG, "initService")
        val configEntity = ConfigEntity(
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.SUB_VERSION,
            BuildConfig.BUILD_NUMBER,
            BuildConfig.BUILD_TIME,
            BuildConfig.BUILD_TYPE,
            BuildConfig.FLAVOR,
            BuildConfig.IS_JENKINS_ENV
        )
        sdkMgrImpl.init(app = app, config = configEntity)

    }

}