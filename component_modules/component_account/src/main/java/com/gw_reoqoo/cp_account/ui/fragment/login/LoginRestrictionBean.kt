package com.gw_reoqoo.cp_account.ui.fragment.login

/**
 * @anchor   Allen
 * @date     2026/4/17 11:59
 * @desc     登录限制数据类
 */
data class LoginRestrictionBean(
    /**
     * 达到的失败限制次数
     */
    val limitTimes: String,
    /**
     * 剩余封禁时间(秒)
     */
    val disableTimespan: Int
)

/**
 * 用于GSON解析的临时数据类
 */
data class HttpRespLoginRestriction(
    val code: Int = 0,
    val msg: String? = null,
    val data: LoginRestrictionBean? = null
)
