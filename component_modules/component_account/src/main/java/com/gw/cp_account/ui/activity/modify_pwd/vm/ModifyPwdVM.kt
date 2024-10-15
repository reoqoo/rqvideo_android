package com.gw.cp_account.ui.activity.modify_pwd.vm

import androidx.lifecycle.viewModelScope
import com.gw.cp_account.api.kapi.IAccountMgrApi
import com.gw.cp_account.repository.UserInfoRepository
import com.gw.lib_base_architecture.ToastIntentData
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gw.lib_http.error.ResponseCode
import com.gwell.loglibs.GwellLogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.gw.resource.R as RR

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/14 1:56
 * Description: ModifyPwdVM
 */
@HiltViewModel
class ModifyPwdVM @Inject constructor() : ABaseVM() {

    companion object {
        private const val TAG = "ModifyPwdVM"
    }

    @Inject
    lateinit var userResp: UserInfoRepository

    @Inject
    lateinit var accountMgrApi: IAccountMgrApi

    fun modifyPwd(oldPwd: String, newPwd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userResp.modifyPwd(oldPwd, newPwd)
                .onSuccess {
                    toastIntentData.postValue(ToastIntentData(RR.string.AA0295))
                    userLogout()
                }
                .onServerError { code, msg ->
                    GwellLogUtils.e(TAG, "modifyPwd: code $code, msg $msg")
                    when (val resp = ResponseCode.getRespCode(code)) {
                        null -> Unit
                        ResponseCode.CODE_12005 -> {
                            toastIntentData.postValue(ToastIntentData(RR.string.AA0294))
                        }

                        else -> {
                            toastIntentData.postValue(ToastIntentData(resp.msgRes))
                        }
                    }
                }
                .onLocalError {
                    GwellLogUtils.e(TAG, "modifyPwd: t ${it.message}")
                    toastIntentData.postValue(ToastIntentData(str = it.message ?: "unknown error"))
                }
        }
    }

    /**
     * 用户退出登录
     */
    private fun userLogout() {
        accountMgrApi.logout()
    }

}