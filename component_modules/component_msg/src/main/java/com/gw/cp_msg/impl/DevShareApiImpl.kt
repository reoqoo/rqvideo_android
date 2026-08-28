package com.gw.cp_msg.impl

import android.app.ActivityManager
import androidx.lifecycle.LifecycleOwner
import com.gw.cp_msg.api.kapi.IDevShareApi
import com.gw_reoqoo.component_family.api.interfaces.IShareDeviceApi
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_lifecycle.activity_lifecycle.ActivityLifecycleManager
import com.therouter.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.isNullOrEmpty

@Singleton
class DevShareApiImpl @Inject constructor(
    val devShareParseImpl: DevShareParseImpl,
    val shareDevApi: IShareDeviceApi
) : IDevShareApi {

    companion object {
        private const val TAG = "DevShareApiImpl"
    }

    override suspend fun openInviteUrl(inviteUrl: String) {
        GwellLogUtils.i(TAG, "openInviteUrl $inviteUrl")
        devShareParseImpl.devShareMsg(inviteUrl)?.run {
            val inviteCode = this["inviteCode"]
            val deviceID = this["deviceID"]
            val sharerName = this["sharerName"]
            if (inviteCode.isNullOrEmpty() || deviceID.isNullOrEmpty()) {
                GwellLogUtils.e(
                    TAG,
                    "devShare fail, inviteCode $inviteCode, deviceID $deviceID is null"
                )
                return@run
            }
            val topActivity = ActivityLifecycleManager.getResumeActivity()
            if (topActivity == null) {
                GwellLogUtils.e(TAG, "devShare fail, topActivity is null")
                return@run
            }
            MainScope().launch(Dispatchers.Main) {
                val dialogOwner = topActivity as LifecycleOwner
                shareDevApi.showShareDetailDialog(
                    owner = dialogOwner,
                    inviteToken = inviteCode,
                    deviceId = deviceID,
                    sharerName = sharerName,
                    onClickAccept = {}
                )
            }
        }
    }

    override suspend fun acceptShareDeviceByUrl(inviteUrl: String): Int {
        GwellLogUtils.i(TAG, "acceptShareDeviceByUrl $inviteUrl")
        val params = devShareParseImpl.devShareMsg(inviteUrl) ?: return -1
        val inviteCode = params["inviteCode"]
        val deviceID = params["deviceID"]
        if (inviteCode.isNullOrEmpty() || deviceID.isNullOrEmpty()) {
            GwellLogUtils.e(TAG, "devShare fail, inviteCode $inviteCode, deviceID $deviceID is null")
            return -1
        }
        return shareDevApi.acceptShareDevice(inviteCode, deviceID)
    }

    override suspend fun rejectShareDeviceByUrl(inviteUrl: String): Int {
        GwellLogUtils.i(TAG, "rejectShareDeviceByUrl $inviteUrl")
        val params = devShareParseImpl.devShareMsg(inviteUrl) ?: return -1
        val inviteCode = params["inviteCode"]
        val deviceID = params["deviceID"]
        if (inviteCode.isNullOrEmpty() || deviceID.isNullOrEmpty()) {
            GwellLogUtils.e(TAG, "devShare fail, inviteCode $inviteCode, deviceID $deviceID is null")
            return -1
        }
        return shareDevApi.rejectShareDevice(deviceID, inviteCode)
    }

}
