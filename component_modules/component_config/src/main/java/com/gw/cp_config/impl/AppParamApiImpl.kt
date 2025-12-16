package com.gw.cp_config.impl

import com.gw.cp_config.BuildConfig
import com.gw.cp_config.api.IAppParamApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/26 15:37
 * Description: AppParamApiImpl
 */
@Singleton
class AppParamApiImpl @Inject constructor() : IAppParamApi {

    /**
     * appID 值
     */
    private var APP_ID = ""

    /**
     * appToken 值
     */
    private var APP_TOKEN = ""

    /**
     * appName 值（这个名称是协议的参数，与app的名称是两个不同的数据）
     */
    private var APP_NAME = ""

    /**
     * 客户ID
     */
    private var APP_CID = ""

    /**
     * 反馈邮箱
     */
    private var feedbackEmail: String? = null


    override fun getAppID(): String {
        return APP_ID
    }

    override fun setAppID(appId: String) {
        APP_ID = appId
    }

    override fun getAppToken(): String {
        return APP_TOKEN
    }

    override fun setAppToken(appToken: String) {
        APP_TOKEN = appToken
    }

    override fun getAppName(): String {
        return APP_NAME
    }

    override fun setAppName(appName: String) {
        APP_NAME = appName
    }

    override fun getCid(): String {
        return APP_CID
    }

    override fun setCid(cid: String) {
        APP_CID = cid
    }

    override fun getFeedbackEmail(): String? {
        return feedbackEmail
    }

    override fun setFeedbackEmail(email: String?) {
        this.feedbackEmail = email
    }
}