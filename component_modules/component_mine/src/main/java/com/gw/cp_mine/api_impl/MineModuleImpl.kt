package com.gw.cp_mine.api_impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gw.cp_config.api.AppChannelName
import com.gw.cp_config.api.IAppParamApi
import com.gw.cp_mine.api.kapi.IMineModuleApi
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_utils.package_utils.LaunchAppUtils
import com.reoqoo.component_iotapi_plugin_opt.api.IGWIotOpt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/21 22:53
 * Description: MineModuleImpl
 */
@Singleton
class MineModuleImpl @Inject constructor() : IMineModuleApi {
    companion object {
        private const val TAG = "MineModuleImpl"

    }

    @Inject
    lateinit var appParamApi: IAppParamApi

    @Inject
    lateinit var gwIotOpt: IGWIotOpt

    override fun appVersionUpgrade(context: Context) {
        try {
            if (AppChannelName.isXiaotunApp(appParamApi.getAppName())) {
                openPhoneBrowser(context, gwIotOpt.getAboutVersionUrl())
            } else {
                LaunchAppUtils.lunchGoogleMarket(context)
            }

        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "appVersionUpgrade fail: reason ${e.message}")
            if (!AppChannelName.isXiaotunApp(appParamApi.getAppName())) {
                openPhoneBrowser(context, gwIotOpt.getAboutVersionUrl())
            }

        }
    }

    /**
     * 使用手机浏览器打开应用市场
     * @param context Context
     * @param url String 手机浏览器需要访问的地址
     */
    override fun openPhoneBrowser(context: Context, url: String) {
        GwellLogUtils.i(TAG, "openPhoneBrowser url: $url")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}