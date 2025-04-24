package com.gw.reoqoo.app.crash

import android.app.Application
import com.gw.reoqoo.BuildConfig
import com.gw.reoqoosdk.sdk.repository.ConfigEntity
import com.jwkj.base_crash.IAppInfoCallback

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/12/20 11:19
 * Description: CrashCallbackImpl
 */
class CrashCallbackImpl(
    private val app: Application,
    private val mConfig: ConfigEntity
) : IAppInfoCallback {
    override fun appBuildNum(): String {
        return com.gw.reoqoosdk.BuildConfig.SDK_BUILD_NUMBER
    }

    override fun appBuildTime(): String {
        return com.gw.reoqoosdk.BuildConfig.SDK_BUILD_TIME
    }

    override fun appBuildType(): String {
        return com.gw.reoqoosdk.BuildConfig.BUILD_TYPE
    }

    override fun appContext(): Application {
        return app
    }

    override fun appJenkinsBuild(): Boolean {
        return com.gw.reoqoosdk.BuildConfig.IS_JENKINS_ENV
    }

    override fun appSubVersion(): String {
        return "101"
    }

    override fun appVersionCode(): String {
        return com.gw.reoqoosdk.BuildConfig.SDK_VERSION_CODE
    }

    override fun appVersionName(): String {
        return com.gw.reoqoosdk.BuildConfig.SDK_VERSION_NAME
    }

    override fun flavorName(): String {
        return com.gw.reoqoosdk.BuildConfig.FLAVOR
    }

    override fun isRelease(): Boolean {
        return !com.gw.reoqoosdk.BuildConfig.DEBUG
    }

    override fun packageName(): String {
        return mConfig.appId
    }
}