package com.gw.reoqoo

import android.app.Application
import com.gw.component_website.api.interfaces.IWebsiteApi
import com.gw.cp_config.api.IAppParamApi
import com.gw.cp_mine.api.kapi.ILocaleApi
import com.gw.reoqoo.app.MainProcessApp
import com.gw.reoqoo.app.api.IReoqooSdkService
import com.gw_reoqoo.cp_account.api.kapi.IInterfaceSignApi
import com.gw_reoqoo.lib_utils.version.VersionUtils
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.iotvideo.init.IoTVideoInitializer
import com.reoqoo.component_iotapi_plugin_opt.api.AppConfig
import com.reoqoo.main.BuildConfig
import com.tencentcs.iotvideo.accountmgr.AccountMgr
import com.tencentcs.iotvideo.accountmgr.IIoTVideoAbility
import com.tencentcs.iotvideo.http.interceptor.RedirectType
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
    lateinit var localeApi: ILocaleApi

    @Inject
    lateinit var appParamApi: IAppParamApi

    @Inject
    lateinit var accountMgr: AccountMgr

    @Inject
    lateinit var signApi: IInterfaceSignApi

    @Inject
    lateinit var websiteApi: IWebsiteApi

    @Inject
    lateinit var processApp: MainProcessApp

    override fun initSync() {
        processApp.initSync(app)
    }

    override suspend fun initService() {
        processApp.mount(app)
    }

    override fun initAccountMgr() {
        val countryCode = localeApi.getCurrentCountry()
        val versionName = AppConfig.instance.VERSION_NAME
        GwellLogUtils.i(TAG, "accountMgr.init: ${appParamApi.getCid()}")
        accountMgr.init(
            "",
            app.packageName,
            VersionUtils.getServiceVersion(appParamApi.getCid(), versionName),
            AccountMgr.PLATFORM_REOQOO,
            BuildConfig.IS_GOOGLE,
            "",
            countryCode,
            object : IIoTVideoAbility {
                override fun getAnonymousSecureKey(): Array<String> {
                    return signApi.getAnonymousInfo(
                        app,
                        appParamApi.getAppID(),
                        versionName
                    )
                }

                /**
                 * 版本支持的功能项
                 * bit0为智能对讲
                 */
                override fun getVersionFuncSupport(): Long? = 1L

                override fun onRedirectRequest(type: RedirectType): String {
                    if (type == RedirectType.SAAS_VAS) {
                        return websiteApi.getPluginHost()
                    }
                    return ""
                }

                override fun sha1WithBase256(signContent: String, accessToken: String): String {
                    return IoTVideoInitializer.p2pAlgorithm.sha1WithBase256(
                        signContent,
                        accessToken
                    )
                }
            }
        )
        GwellLogUtils.i(TAG, "initAccountMgr init countryCode:$countryCode versionName:$versionName")
        // 设置地区
        accountMgr.setRegion(countryCode)
        // 设置appName,appID,appToken
        accountMgr.setPlatformInfo(
            appParamApi.getAppName(),
            appParamApi.getAppID(),
            appParamApi.getAppToken()
        )
    }

}