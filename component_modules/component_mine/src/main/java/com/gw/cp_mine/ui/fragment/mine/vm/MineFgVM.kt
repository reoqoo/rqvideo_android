package com.gw.cp_mine.ui.fragment.mine.vm

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gw.component_family.api.interfaces.FamilyModeApi
import com.gw.cp_account.api.kapi.IAccountApi
import com.gw.cp_account.api.kapi.IUserInfo
import com.gw.cp_mine.R
import com.gw.cp_mine.entity.MenuListEntity
import com.gw.cp_msg.api.kapi.IMsgExternalApi
import com.gw.lib_base_architecture.PageJumpData
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gw.lib_router.ReoqooRouterPath
import com.gw.reoqoosdk.dev_monitor.IMonitorService
import com.gw.reoqoosdk.dev_upgrade.IDevUpgradeService
import com.gwell.loglibs.GwellLogUtils
import com.therouter.TheRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.gw.resource.R as RR

/**
@author: xuhaoyuan
@date: 2023/8/18
description:
1.
 */
@HiltViewModel
class MineFgVM @Inject constructor(
    private var accountApi: IAccountApi,
    private var pluginManager: IMonitorService,
    private var familyModeApi: FamilyModeApi,
    private val upgradeApi: IDevUpgradeService,
    private val msgApi: IMsgExternalApi,
) : ABaseVM() {

    companion object {
        private const val TAG = "MineFgVM"
    }

    /**
     * 用户信息同步
     */
    val userInfo = MutableLiveData<IUserInfo>()

    /**
     * 消息中心小红点状态
     */
    val redPointState = MutableLiveData<Boolean>()

    /**
     * 是否支持云服务
     */
    val isSupportCloud = MutableLiveData<Boolean>()

    /**
     * 是否包含4G设备
     */
    val isSupport4G = MutableLiveData<Boolean>()

    /**
     * 设置项item数据
     */
    private val menuItems = listOf(
        MenuListEntity(
            R.drawable.mine_ic_feedback,
            RR.string.AA0223,
            ReoqooRouterPath.MinePath.ACTIVITY_FEEDBACK
        ),
        MenuListEntity(
            R.drawable.mine_ic_about,
            RR.string.AA0224,
            ReoqooRouterPath.MinePath.ACTIVITY_ABOUT
        )
    )

    fun initLMenuList() = menuItems

    fun watchUserInfo(owner: LifecycleOwner) {
        accountApi.watchUserInfo().observe(owner) {
            userInfo.postValue(it)
        }
    }

    fun updateUserInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            accountApi.getRemoteUserInfo()
        }
    }

    fun checkAreaCloudServer() {
        viewModelScope.launch(Dispatchers.IO) {
            accountApi.getAsyncUserId()?.let {
                val device = familyModeApi.getDeviceList(it).firstOrNull()
                if (device == null || familyModeApi.deviceSupportCloud(deviceId = device.deviceId) != true) {
                    isSupportCloud.postValue(false)
                }
            } ?: GwellLogUtils.e(TAG, "startAlbumPage error: userID is null")
        }
    }

    /**
     * 是否包含4G设备
     */
    fun checkHas4GDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            accountApi.getAsyncUserId()?.let {
                val deviceList = familyModeApi.getDeviceList(it)
                val device4G =
                    deviceList.filter { device -> familyModeApi.is4GDevice(deviceId = device.deviceId) == true }
                isSupport4G.postValue(device4G.isNotEmpty())
            } ?: let {
                GwellLogUtils.e(TAG, "deviceList has no 4g device")
                isSupport4G.postValue(false)
            }
        }
    }

    /**
     * 初始化升级信息
     */
    fun initUpgradeInfo() {
        upgradeApi.refreshUpgradeState()
    }

    /**
     * 小红点状态
     */
    private val unReadMsgCount: ((Int) -> Unit) = { unReadCount ->
        redPointState.postValue(unReadCount > 0)
    }

    /**
     * 系统通知的未读消息数量
     */
    fun updateMsgRedPoint() {
        msgApi.getUnreadMsgCount(unReadMsgCount)
    }

    /**
     * 打开相册
     */
    fun startAlbumPage() {
        viewModelScope.launch(Dispatchers.IO) {
            accountApi.getAsyncUserId()?.let {
                val deviceList = familyModeApi.getDeviceList(it).map { device ->
                    mapOf(Pair("deviceId", device.deviceId), Pair("deviceName", device.remarkName))
                }
                GwellLogUtils.i(TAG, "deviceList: $deviceList")
                pluginManager.startAlbumActivity(Gson().toJson(deviceList), "")
            } ?: GwellLogUtils.e(TAG, "startAlbumPage error: userID is null")
        }
    }

    /**
     * 页面跳转
     *
     * @param routerPath String 页面路径
     */
    fun jumpToNext(routerPath: String) {
        pageJumpData.postValue(PageJumpData(TheRouter.build(routerPath)))
    }
}