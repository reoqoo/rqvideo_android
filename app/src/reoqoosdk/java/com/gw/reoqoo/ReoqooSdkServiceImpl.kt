package com.gw.reoqoo

import android.app.Application
import com.gw.cp_config.api.IAppParamApi
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

    @Inject
    lateinit var appParamApi: IAppParamApi

    override fun initService() {
        GwellLogUtils.i(TAG, "initService")
        val configEntity = ConfigEntity(
            appParamApi.getAppID(),
            appParamApi.getAppToken(),
            appParamApi.getAppName(),
            BuildConfig.VERSION_NAME,
            BuildConfig.APPLICATION_ID,
        )
        sdkMgrImpl.init(app = app, config = configEntity)

    }

}