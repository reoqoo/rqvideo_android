package com.gw_reoqoo.component_family.ui.recv_scan_share

import androidx.lifecycle.viewModelScope
import com.gw.cp_config.api.IAppConfigApi
import com.gw.cp_config.api.ProductImgType
import com.gw.cp_config_net.api.interfaces.DevShareConstant
import com.gw_reoqoo.component_family.api.interfaces.FamilyModeApi
import com.gw_reoqoo.component_family.repository.DeviceRepository
import com.gw_reoqoo.cp_account.api.kapi.IAccountApi
import com.gw_reoqoo.lib_base_architecture.vm.ABaseVM
import com.gw_reoqoo.lib_http.entities.ScanShareQRCodeResult
import com.gwell.loglibs.GwellLogUtils
import com.reoqoo.component_iotapi_plugin_opt.api.IGWIotOpt
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class RecvScanShareVM @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val igwIotOpt: IGWIotOpt,
    private val accountApi: IAccountApi,
    private val configApi: IAppConfigApi,
    private val familyModeApi: FamilyModeApi
) : ABaseVM() {
    companion object {
        private const val TAG = "RecvScanShareVM"
        private const val TIMEOUT_MS = 5000L
    }

    private var deviceId: String = ""
    private var solution: String? = null

    private val _scanShareFlow = MutableStateFlow<HttpAction<ScanShareQRCodeResult>>(
        HttpAction.Loading()
    )
    val scanShareFlow: StateFlow<HttpAction<ScanShareQRCodeResult>> = _scanShareFlow

    /**
     * 发起扫码分享添加设备
     * @param params 扫码分享参数
     */
    fun requestShare(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeout(TIMEOUT_MS) {
                    _scanShareFlow.emit(HttpAction.Loading())
                    configApi.updateConfigSync()

                    val flow = addDeviceByScanShareCode(params)
                    if (flow == null) {
                        _scanShareFlow.emit(
                            HttpAction.Fail(Exception("invalid qrcode params"))
                        )
                        return@withTimeout
                    }
                    flow.collect { action ->
                        when (action) {
                            is HttpAction.Success -> {
                                deviceId = action.data?.devId ?: ""
                                solution = configApi.getProductSolution(action.data?.pid.toString(), action.data?.productModel)
                                safelyRefreshDevices()
                                _scanShareFlow.emit(action)
                            }

                            is HttpAction.Fail, is HttpAction.Loading -> {
                                _scanShareFlow.emit(action)
                            }
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                GwellLogUtils.e(TAG, "requestShare timeout", e)
                _scanShareFlow.emit(HttpAction.Fail(Exception("timeout")))
            }
        }
    }

    fun getProductImgWithPID(pid: String, productModel: String?): String? {
        val imgUrl =
            configApi.getProductImgUrl_D(pid, productModel, ProductImgType.INTRODUCTION)
        return imgUrl
    }

    /**
     * 扫码分享接口
     */
    private fun addDeviceByScanShareCode(
        params: Map<String, String>
    ): Flow<HttpAction<ScanShareQRCodeResult>>? {

        val qrcodeToken = params[DevShareConstant.PARAMS_INVITE_CODE]
        val pid = params[DevShareConstant.PARAM_PID_KEY]
        val deviceId = params[DevShareConstant.PARAMS_DEVICE_ID] ?: ""
        GwellLogUtils.i(
            TAG,
            "addDeviceByScanShareCode params=$params, pid=$pid, deviceId=$deviceId"
        )

        if (qrcodeToken.isNullOrEmpty()) return null

        return familyModeApi.addDeviceByScanShareCode(qrcodeToken, deviceId)
    }

    /**
     * 刷新设备
     */
    private suspend fun safelyRefreshDevices() {
        try {
            igwIotOpt.refreshDevices()
            deviceRepository.loadDeviceFromRemote()
        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "refreshDevices failed", e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun openHome() {
        // 这里任务有一定耗时，需要用户点击立即查看以后马上关闭Activity，如果用viewmodelScope会在
        // 调用finish Activity以后这个任务会被异常终止，导致打开主页失败，所以使用了应用级别的scope
        GlobalScope.launch(Dispatchers.IO) {
            finishActivityLD.postValue(true)
            // 设置一个超时时间，防止打开主页失败导致内存泄漏
            withTimeoutOrNull(TIMEOUT_MS) {
                igwIotOpt.openHome(deviceId, solution)
            }
        }
    }
}