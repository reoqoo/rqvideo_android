package com.gw.cp_account.kits

import android.content.Context
import android.text.TextUtils
import com.gw.cp_account.utils.AppTagUtils
import com.gw.lib_http.HiltApi
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_utils.local.LocalUtils
import com.tencentcs.iotvideo.IoTVideoSdk

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/8/11 9:48
 * Description: 匿名签名工具类
 */
object AccountMgrKit {

    private const val TAG = "AccountMgrKit"

    /**
     * 设置匿名签名信息
     * 主要调用页面：1、RegisterActivity.onCreate/onNewIntent
     *            2、ResetEmailPwdActivity.onCreate
     */
    fun setAnonymousSecureSecretInfo(context: Context, appId: String) {

    }


    /**
     * 设置AccessInfo
     * 主要使用页面：1、LoginActivity/RegisterActivity.onDestroy 中 使用
     *
     * @param accessId String?
     * @param accessToken String?
     */
    fun setAccessInfo(accessId: String?, accessToken: String?) {
        val accountMgr = HiltApi.accountMgr
        if (!TextUtils.isEmpty(accessId) && !TextUtils.isEmpty(accessToken)) {
            accountMgr.setAccessInfo(accessId, accessToken)
            accountMgr.setRegion(LocalUtils.getCountry())
        } else {
            GwellLogUtils.e(TAG, "setAccessInfo failure, accessId is null, accessToken is null")
        }
    }

    /**
     * 设置用户地区
     *
     * @param area String? 地区
     */
    fun setUserArea(area: String?) {
        if (!area.isNullOrEmpty()) {
            val accountMgr = HiltApi.accountMgr
            accountMgr.setUserArea(area)
        }
    }

    /**
     * 设置用户注册地
     *
     * @param regRegion String? 注册地二字码
     */
    fun setRegRegion(regRegion: String?) {
        val accountMgr = HiltApi.accountMgr
        if (regRegion.isNullOrEmpty()) {
            accountMgr.setRegRegion("")
        } else {
            accountMgr.setRegRegion(regRegion)
        }
    }

    /**
     * 获取匿名签名信息
     *
     * @param context Context
     * @param appId String
     * @return Array<String>
     */
    fun getAnonymousSecureKey(context: Context, appId: String, versionName: String): Array<String> {
        return IoTVideoSdk.getAnonymousSecureKey(AppTagUtils.getAppTag(context, appId, versionName))
    }
}