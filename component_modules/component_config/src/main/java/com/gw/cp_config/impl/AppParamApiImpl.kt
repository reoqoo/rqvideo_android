package com.gw.cp_config.impl

import com.gw.cp_config.api.IAppParamApi
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/26 15:37
 * Description: AppParamApiImpl
 */
class AppParamApiImpl @Inject constructor() : IAppParamApi {

    companion object {

        /**
         * appID 值
         */
        private const val APP_ID = "244d1828c953492dacb0ca5201f54221"

        /**
         * appToken 值
         */
        private const val APP_TOKEN =
            "0bedfb9f0f8404683a58ff636b7b8c8f0c5e809d72ab9cdc1ed5a64782b9a315"

        /**
         * appName 值（这个名称是协议的参数，与app的名称是两个不同的数据）
         */
        private const val APP_NAME = "ipTIME CCTV_android"

    }

    override fun getAppID(): String {
        return APP_ID
    }

    override fun getAppToken(): String {
        return APP_TOKEN
    }

    override fun getAppName(): String {
        return APP_NAME
    }

}