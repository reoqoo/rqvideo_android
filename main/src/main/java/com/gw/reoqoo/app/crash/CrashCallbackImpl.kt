package com.gw.reoqoo.app.crash

import android.app.Application
import com.reoqoo.main.BuildConfig
import com.jwkj.base_crash.IAppInfoCallback
import com.reoqoo.component_iotapi_plugin_opt.api.AppConfig

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/12/20 11:19
 * Description: CrashCallbackImpl
 */
class CrashCallbackImpl(val app: Application) : IAppInfoCallback {
    override fun appBuildNum(): String {
        return AppConfig.instance.BUILD_NUMBER
    }

    override fun appBuildTime(): String {
        return AppConfig.instance.BUILD_TIME
    }

    override fun appBuildType(): String {
        return BuildConfig.BUILD_TYPE
    }

    override fun appContext(): Application {
        return app
    }

    override fun appJenkinsBuild(): Boolean {
        return AppConfig.instance.IS_JENKINS_ENV
    }

    override fun appSubVersion(): String {
        return AppConfig.instance.SUB_VERSION
    }

    override fun appVersionCode(): String {
        return AppConfig.instance.VERSION_CODE.toString()
    }

    override fun appVersionName(): String {
        return AppConfig.instance.VERSION_NAME
    }

    override fun flavorName(): String {
        return BuildConfig.FLAVOR
    }

    override fun isRelease(): Boolean {
        return !BuildConfig.DEBUG
    }

    override fun packageName(): String {
        return app.packageName
    }
}