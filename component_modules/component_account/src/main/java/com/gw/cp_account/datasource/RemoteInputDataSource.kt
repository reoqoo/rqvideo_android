package com.gw.cp_account.datasource

import com.gw.cp_account.api.impl.AccountMgrImpl
import com.gw.cp_account.http.PhoneCodeResponse
import com.gw.lib_http.*
import com.gw.lib_http.entities.CheckAccountExistResponse
import com.gw.lib_http.wrapper.HttpServiceWrapper
import com.gwell.loglibs.GwellLogUtils
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/5 23:13
 * Description: 接口请求数据源
 */
class RemoteInputDataSource @Inject constructor(
    private val httpService: HttpServiceWrapper,
    private val accountMgrImpl: AccountMgrImpl
) {

    companion object {
        private const val TAG = "RemoteInputDataSource"
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email String 邮箱地址
     * @return RespResult<CheckAccountExistResponse> 接口回调
     */
    suspend fun checkEmailExist(
        email: String,
    ): RespResult<CheckAccountExistResponse> {
        val result = Channel<RespResult<CheckAccountExistResponse>>(1)
        httpService.checkEmailExist(email, typeSubscriber<CheckAccountExistResponse>(
            onSuccess = { _data ->
                result.trySend(RespResult.Success(_data))
            },
            onFail = { t ->
                if (t is ResponseNotSuccessException) {
                    result.trySend(RespResult.ServerError(t.code, t.message))
                } else {
                    GwellLogUtils.e(TAG, "checkEmailExist onFail: error = ${t.message}")
                    result.trySend(RespResult.LocalError(t))
                }
            }
        ))
        return result.receive()
    }

    /**
     * 检查手机号是否存在
     *
     * @param mobileArea String 手机区号
     * @param mobile String 手机号
     * @return RespResult<CheckAccountExistResponse> 回调
     */
    suspend fun checkMobileExist(
        mobileArea: String,
        mobile: String,
    ): RespResult<CheckAccountExistResponse> {
        val result = Channel<RespResult<CheckAccountExistResponse>>(1)
        httpService.checkMobileExist(mobileArea, mobile, typeSubscriber<CheckAccountExistResponse>(
            onSuccess = { _data ->
                result.trySend(RespResult.Success(_data))
            },
            onFail = { t ->
                if (t is ResponseNotSuccessException) {
                    result.trySend(RespResult.ServerError(t.code, t.message))
                } else {
                    GwellLogUtils.e(TAG, "checkEmailExist onFail: error = ${t.message}")
                    result.trySend(RespResult.LocalError(t))
                }
            }
        ))
        return result.receive()
    }

    /**
     * 验证码校验接口
     *
     * @param countryCode String 国家码
     * @param phoneNumber String 手机号
     * @param type Int 用途（0：注册 1：忘记密码）
     * @return RespResult<PhoneCodeResponse> 回调
     */
    suspend fun registerByPhone(
        countryCode: String,
        phoneNumber: String,
        type: Int,
        isLogin: Boolean
    ): RespResult<PhoneCodeResponse> {
        val result = Channel<RespResult<PhoneCodeResponse>>(1)
        val listener = typeSubscriber<PhoneCodeResponse>(
            onSuccess = { _data ->
                result.trySend(RespResult.Success(_data))
            },
            onFail = { t ->
                if (t is ResponseNotSuccessException) {
                    result.trySend(RespResult.ServerError(t.code, t.message))
                } else {
                    GwellLogUtils.e(TAG, "checkEmailExist onFail: error = ${t.message}")
                    result.trySend(RespResult.LocalError(t))
                }
            }
        )
        if (isLogin) {
            httpService.mobileCheckCodeWithLogin(
                countryCode,
                phoneNumber,
                type,
                null,
                null,
                listener
            )
        } else {
            httpService.mobileCheckCode(
                countryCode,
                phoneNumber,
                type,
                null,
                null,
                listener
            )
        }

        return result.receive()
    }

}