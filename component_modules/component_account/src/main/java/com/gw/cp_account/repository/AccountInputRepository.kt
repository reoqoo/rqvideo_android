package com.gw.cp_account.repository

import com.gw.cp_account.datasource.RemoteInputDataSource
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/5 23:10
 * Description: 账号输入的数据层
 */
class AccountInputRepository @Inject constructor(
    private val remoteDS: RemoteInputDataSource
) {

    companion object {
        private const val TAG = "AccountInputRepository"
    }


    /**
     * 检查邮箱是否存在
     *
     * @param email String 邮箱地址
     * @return RespResult<CheckAccountExistResponse> 接口回调
     */
    suspend fun checkEmailExist(email: String) = remoteDS.checkEmailExist(email)

    /**
     * 检查手机号是否存在
     *
     * @param mobileArea String 手机区号
     * @param mobile String 手机号
     * @return RespResult<CheckAccountExistResponse> 回调
     */
    suspend fun checkMobileExist(mobileArea: String, mobile: String) =
        remoteDS.checkMobileExist(mobileArea, mobile)

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
    ) = remoteDS.registerByPhone(countryCode, phoneNumber, type, isLogin)

}