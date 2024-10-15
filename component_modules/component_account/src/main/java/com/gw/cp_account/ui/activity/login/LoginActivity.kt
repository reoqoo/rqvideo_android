package com.gw.cp_account.ui.activity.login

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.gw.cp_account.R
import com.gw.cp_account.databinding.AccountActivityLoginBinding
import com.gw.cp_account.entity.ParamConstants
import com.gw.lib_base_architecture.view.ABaseMVVMDBActivity
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_router.RouterParam
import com.therouter.router.Autowired
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint


/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/7/28 11:05
 * Description: 登录页面
 */
@AndroidEntryPoint
@Route(
    path = ReoqooRouterPath.AccountPath.LOGIN_ACTIVITY_PATH,
    params = [RouterParam.PARAM_NEED_LOGIN, "false"]
)
class LoginActivity : ABaseMVVMDBActivity<AccountActivityLoginBinding, LoginVM>() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    @JvmField
    @Autowired(name = ParamConstants.PARAM_BIND_TYPE)
    var entrance: Int = 1

    override fun getLayoutId(): Int {
        return R.layout.account_activity_login
    }

    override fun initView() {
    }

    override fun initData(savedInstanceState: Bundle?) {
        super.initData(savedInstanceState)
    }

    override fun <T : ViewModel?> loadViewModel(): Class<T> {
        return LoginVM::class.java as Class<T>
    }

}