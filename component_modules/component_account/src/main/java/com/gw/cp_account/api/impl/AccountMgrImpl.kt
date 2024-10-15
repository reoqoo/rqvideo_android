package com.gw.cp_account.api.impl

import android.app.Application
import com.gw.component_push.api.interfaces.IPushApi
import com.gw.cp_account.api.kapi.IAccountMgrApi
import com.gw.cp_account.kits.AccountMgrKit
import com.gw.cp_account.repository.UserInfoRepository
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_router.navigation
import com.gw.reoqoosdk.monitor.IMonitorService
import com.jwkj.base_lifecycle.activity_lifecycle.ActivityLifecycleManager
import com.jwkj.base_statistics.sa.kits.SA
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/10/17 14:43
 * Description: AccountMgrImpl
 */
class AccountMgrImpl @Inject constructor(
    private val app: Application,
    private val pluginMgr: IMonitorService,
    private val accountApiImpl: AccountApiImpl,
    private val userInfoRepo: UserInfoRepository,
    private val pushApi: IPushApi
) : IAccountMgrApi {

    private val scope by lazy {
        MainScope()
    }

    /**
     * 退出登录
     */
    override fun logout() {
        scope.launch {
            val userInfo = accountApiImpl.getSyncUserInfo()
            val terminalId = userInfo?.terminalId ?: ""
            pushApi.unRegisterPushServer(terminalId)
            userInfoRepo.userLogout(terminalId)
            SA.saLogout()
        }
        setLogoutState()
    }

    /**
     * 登录失效
     */
    override fun loginFailure() {
        // 登录失效和退出登录的区别，是不需要调用退出登录的接口
        scope.launch {
            val userInfo = accountApiImpl.getSyncUserInfo()
            val terminalId = userInfo?.terminalId ?: ""
            pushApi.unRegisterPushServer(terminalId)
            userInfoRepo.loginFailure()
        }
        setLogoutState()
    }

    /**
     *  设置退出的轮毂状态
     */
    private fun setLogoutState() {
        //IoTVideoSdk.unregister()
        AccountMgrKit.setRegRegion("")
        pluginMgr.onAccountExit()
        ActivityLifecycleManager.finishAllActivity()
        ReoqooRouterPath.AccountPath.LOGIN_ACTIVITY_PATH.navigation(app)
    }

}