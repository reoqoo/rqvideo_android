package com.gw.cp_msg.ui.activity.msg_info.vm

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gw.component_device_share.api.DevShareApi.Companion.PARAM_DEV_SHARE_ENTITY
import com.gw.component_family.api.interfaces.FamilyModeApi
import com.gw.cp_config_net.entity.ShareQRCodeEntity
import com.gw.cp_msg.entity.http.MsgDetailEntity
import com.gw.cp_msg.entity.http.MsgInfoListEntity
import com.gw.cp_msg.repository.MsgInfoRepository
import com.gw.lib_base_architecture.PageJumpData
import com.gw.lib_base_architecture.ToastIntentData
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gw.lib_http.HttpErrUtils
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_router.with
import com.gw.reoqoosdk.cloud_service.ICloudService
import com.gw.reoqoosdk.constant.NetConfigConstant
import com.gw.reoqoosdk.net_config.api.INetConfigService
import com.gwell.loglibs.GwellLogUtils
import com.therouter.TheRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *@message   MsgInfoVM
 *@user      zouhuihai
 *@date      2022/8/8
 */
@HiltViewModel
class MsgInfoVM @Inject constructor() : ABaseVM() {

    companion object {
        private const val TAG = "MsgInfoVM"

        private const val COUNT_ONE_PAGE = 200

    }

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var repository: MsgInfoRepository

    @Inject
    lateinit var iCloudService: ICloudService

    @Inject
    lateinit var configApi: INetConfigService

    @Inject
    lateinit var familyModeApi: FamilyModeApi


    /**
     * 获取消息详情列表成功
     */
    val msgInfoEvent: MutableLiveData<MutableList<MsgInfoListEntity.MSGInfo>> = MutableLiveData()

    /**
     * 所有的消息详情
     */
    private var msgInfos: MutableList<MsgInfoListEntity.MSGInfo> = ArrayList()

    /**
     * 拉取消息详情
     *
     * @param msg           消息大类
     * @param firstLoad     是否第一次拉取
     */
    fun loadMsgInfo(msg: MsgDetailEntity, lastId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (lastId == 0L) {
                msgInfos.clear()
            }
            repository.loadMsgInfo(msg.tag, lastId, COUNT_ONE_PAGE)
                .onSuccess {
                    this?.run {
                        list?.let { msgInfoList ->
                            msgInfos.addAll(msgInfoList)
                            if (msgInfoList.size < COUNT_ONE_PAGE) {
                                // 已经拉取完了
                                msgInfoEvent.postValue(msgInfos)
                            } else {
                                // 没有拉取完，继续拉取
                                msgInfoList[msgInfoList.size - 1].id.run {
                                    loadMsgInfo(msg, this)
                                }
                            }
                        } ?: let {
                            msgInfoEvent.postValue(msgInfos)
                        }
                    } ?: let {
                        msgInfoEvent.postValue(msgInfos)
                    }
                }
                .onServerError { code, msg ->
                    GwellLogUtils.e(TAG, "loadMsgInfo error: code $code, msg $msg")
                    toastIntentData.postValue(
                        ToastIntentData(
                            str = msg ?: app.getString(HttpErrUtils.showErrorToast(code.toString()))
                        )
                    )
                }
                .onLocalError {
                    GwellLogUtils.e(TAG, "loadMsgInfo error: throwable ${it.message}")
                }
        }

    }

    /**
     * 打开h5网页
     *
     * @param url String? URL地址
     * @param title String 标题
     */
    fun openWebView(url: String?, title: String) {
        if (url.isNullOrEmpty()) {
            GwellLogUtils.e(TAG, "open webView fail, url is null")
            return
        }
        iCloudService.openWebView(url, title)
    }

    /**
     * 设备分享消息解析
     *
     * @param shareUrl String 分享url链接
     * @return Map<String, String>? 参数集
     */
    fun devShareMsg(shareUrl: String): Map<String, String>? {
        shareUrl.run {
            if (isNullOrEmpty()) {
                GwellLogUtils.e(TAG, "redirectUrl is empty")
                return null
            }
            if (this.startsWith("AppNativeUrl?", true)) {
                val url = this.replace(
                    "AppNativeUrl?",
                    ShareQRCodeEntity.HOST_DEV_SHARE
                )
                GwellLogUtils.i(TAG, "url $url")

                val qrcodeEntity = configApi.parseShareUrl(url, INetConfigService.Type.SHARE)
                GwellLogUtils.i(TAG, "qrcodeEntity $qrcodeEntity")
                val inviteCode = qrcodeEntity[NetConfigConstant.PARAMS_INVITE_CODE]
                val deviceID = qrcodeEntity[NetConfigConstant.PARAMS_DEVICE_ID]
                val sharerName = qrcodeEntity[NetConfigConstant.PARAMS_SHARER_NAME]
                if (inviteCode.isNullOrEmpty() || deviceID.isNullOrEmpty()) {
                    GwellLogUtils.e(TAG, "inviteCode $inviteCode, deviceID $deviceID")
                    return null
                }
                return mapOf(
                    "inviteCode" to inviteCode,
                    "deviceID" to deviceID,
                    "sharerName" to (sharerName ?: "")
                )
            }
        }
        return null
    }

    /**
     * 点击跳转到分享管理
     *
     * @param deviceId String 设备ID
     */
    fun goDevManagerPage(deviceId: String) {
        familyModeApi.deviceInfo(deviceId)?.let {
            pageJumpData.postValue(
                PageJumpData(
                    TheRouter.build(
                        ReoqooRouterPath
                            .DevShare
                            .ACTIVITY_SHARE_MANAGER_OWNER_PATH
                    ).with(mapOf(PARAM_DEV_SHARE_ENTITY to it))
                )
            )
        } ?: GwellLogUtils.e(TAG, "goDevManagerPage fail: deviceInfo is null, deviceId = $deviceId")
    }

}