package com.gw.cp_account.utils

import android.content.Context
import com.gw.lib_utils.version.VersionUtils
import com.gwell.loglibs.GwellLogUtils

/**
 * Yoosee中登录使用的工具类
 */
class AppTagUtils {

    companion object {
        private const val TAG = "AppTagUtils"

        val instance: AppTagUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            AppTagUtils()
        }

    }

    fun getAppTag(context: Context, appId: String): String {
        val pm = context.applicationContext.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        GwellLogUtils.i(TAG, "versionName: ${pi.versionName}")
        val appIntStringVersion = VersionUtils.getIntVersion(pi.versionName)
        return appId + appIntStringVersion
    }

}