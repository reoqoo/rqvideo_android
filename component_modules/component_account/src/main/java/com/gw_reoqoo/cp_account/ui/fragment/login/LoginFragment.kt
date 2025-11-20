package com.gw_reoqoo.cp_account.ui.fragment.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.gw.component_website.api.interfaces.IWebsiteApi
import com.gw.component_webview.api.interfaces.IWebViewApi
import com.gw_reoqoo.cp_account.R
import com.gw_reoqoo.cp_account.databinding.AccountFragmentLoginBinding
import com.gw_reoqoo.cp_account.entity.AccountInputType
import com.gw_reoqoo.cp_account.kits.PrivatePolicyStringKit
import com.gw_reoqoo.cp_account.sa.AccountSaEvent
import com.gw_reoqoo.cp_account.ui.fragment.ShareVM
import com.gw.cp_config.api.AppChannelName
import com.gw.cp_config.api.IAppParamApi
import com.gw_reoqoo.lib_base_architecture.ToastIntentData
import com.gw_reoqoo.lib_base_architecture.view.ABaseMVVMDBFragment
import com.gw_reoqoo.lib_router.ReoqooRouterPath
import com.gw_reoqoo.lib_utils.ktx.dp
import com.gw_reoqoo.lib_utils.ktx.invisible
import com.gw_reoqoo.lib_utils.version.VersionUtils
import com.gw_reoqoo.lib_widget.dialog.CommonDialog
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_lifecycle.activity_lifecycle.ActivityLifecycleManager
import com.jwkj.base_statistics.sa.kits.SA
import com.jwkj.base_utils.local.LanguageUtils
import com.jwkj.base_utils.str_utils.GwStringUtils
import com.reoqoo.component_iotapi_plugin_opt.api.IGWIotOpt
import com.gw_reoqoo.lib_http.entities.DistrictEntity
import com.therouter.TheRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.gw_reoqoo.resource.R as RR


/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/8/1 16:22
 * Description: 登录页
 */
@AndroidEntryPoint
class LoginFragment : ABaseMVVMDBFragment<AccountFragmentLoginBinding, LoginFrgVM>() {

    companion object {
        private const val TAG = "LoginFragment"

        /**
         * reoqoo
         */
        private const val REO_QOO = "reoqoo"

        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @Inject
    lateinit var webViewApi: IWebViewApi

    @Inject
    lateinit var websiteApi: IWebsiteApi

    @Inject
    lateinit var appParamApi: IAppParamApi

    @Inject
    lateinit var gwIotOpt: IGWIotOpt

    // 输入框空字符串过滤器
    private val mFilter: InputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            if (source != null && source.toString().trim { it <= ' ' }.isEmpty()) {
                // 如果输入的是空格，则返回空字符串，从而阻止空格输入
                return@InputFilter ""
            }
            null // 允许其他字符输入
        }

    /**
     * 共享VM，主要处理数据回传的问题
     */
    private val shareVM: ShareVM by activityViewModels()

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)

        mViewBinding.tvArea.let {
            it.setOnClickListener {
                val action =
                    LoginFragmentDirections.accountActionAccountLoginfragmentToAccountArealistfragment(
                        shareVM.districtLD.value
                    )
                findNavController().navigate(action)
            }

            it.invisible(AppChannelName.isXiaotunApp(appParamApi.getAppName()))
        }
        GwellLogUtils.i(TAG, "initView xiaotunApp == ${appParamApi.getAppName()}")

        mViewBinding.etPwd.setOnFocusChangeListener { v, hasFocus ->
            mViewBinding.ctlPwd.isSelected = hasFocus
        }

        mViewBinding.tvForgetPwd.setOnClickListener {
            SA.track(AccountSaEvent.LOGIN_FORGOTPASSWORDBUTTONCLICK)
            jumpToAccountInputFragment(AccountInputType.ACCOUNT_FORGET)
        }

        mViewBinding.cbPwdState.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                //如果被选中则显示密码
                mViewBinding.etPwd.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                //TextView默认光标在最左端，这里控制光标在最右端
                mViewBinding.etPwd.setSelection(mViewBinding.etPwd.text?.length ?: 0)
            } else {
                //如果没选中CheckBox则隐藏密码
                mViewBinding.etPwd.transformationMethod = PasswordTransformationMethod.getInstance()
                mViewBinding.etPwd.setSelection(mViewBinding.etPwd.text?.length ?: 0)
            }
        }

        mViewBinding.tvRegister.setOnClickListener {
            SA.track(AccountSaEvent.REGISTER_BUTTONCLICK)
            if (AppChannelName.isXiaotunApp(appParamApi.getAppName())) {
                jumpToAccountInputFragment(AccountInputType.ACCOUNT_REGISTER)
            } else {
                val action =
                    LoginFragmentDirections.accountActionAccountLoginfragmentToAccountRegisterareafragment(
                        shareVM.districtLD.value
                    )
                findNavController().navigate(action)
            }
        }

        mViewBinding.tvAgree.movementMethod = LinkMovementMethod.getInstance()
        mViewBinding.tvAgree.text =
            PrivatePolicyStringKit.getAgreement(requireActivity(), false, showUserProtocol = {
                val userProtocol = websiteApi.getUserProtocolUrl()
                GwellLogUtils.i(TAG, "userProtocol $userProtocol")
                webViewApi.openWebView(userProtocol, getString(RR.string.AA0044))
            }, showPrivacyProtocol = {
                val userPrivacyUrl = websiteApi.getUserPrivacyUrl()
                GwellLogUtils.i(TAG, "userPrivacyUrl $userPrivacyUrl")
                webViewApi.openWebView(userPrivacyUrl, getString(RR.string.AA0368))
            })

        mViewBinding.imgAgree.setOnCheckedChangeListener { _, isChecked ->
            mFgViewModel.agreeBtnClick(isChecked)
        }

        mViewBinding.btnLogin.setOnClickListener {
            val account = mViewBinding.etAccount.text.toString().trim()
            GwellLogUtils.i(TAG, "account = $account")
            val pwd = mViewBinding.etPwd.text.toString()
            GwellLogUtils.i(TAG, "pwd = $pwd")
            if (account.isEmpty()) {
                toast.show(RR.string.AA0013)
                return@setOnClickListener
            }
            if (pwd.isEmpty()) {
                toast.show(RR.string.AA0013)
                return@setOnClickListener
            }
            if (!GwStringUtils.isMobileNO(account) &&
                !GwStringUtils.isEmail(account)
            ) {
                toast.show(RR.string.AA0012)
                return@setOnClickListener
            }
            if (null == shareVM.districtLD.value) {
                mFgViewModel.toastIntentData.postValue(ToastIntentData(com.gw_reoqoo.resource.R.string.AA0017))
                GwellLogUtils.e(TAG, "地区信息初始化失败")
                return@setOnClickListener
            }
            if (gwIotOpt.sdkInitFinish() != true) {
                GwellLogUtils.i(TAG, "btnLogin onclick sdkInitFinish != true SDK未初始化完成")
                return@setOnClickListener
            }
            GwellLogUtils.i(TAG, "isAgree = ${mFgViewModel.isAgree.value}")
            if (mFgViewModel.isAgree.value == false) {
                context?.let {
                    val content = PrivatePolicyStringKit.getAgreement(
                        it,
                        false,
                        showUserProtocol = {
                            val userProtocol = websiteApi.getUserProtocolUrl()
                            GwellLogUtils.i(TAG, "userProtocol $userProtocol")
                            webViewApi.openWebView(userProtocol, getString(RR.string.AA0044))
                        },
                        showPrivacyProtocol = {
                            val userPrivacyUrl = websiteApi.getUserPrivacyUrl()
                            GwellLogUtils.i(TAG, "userPrivacyUrl $userPrivacyUrl")
                            webViewApi.openWebView(
                                userPrivacyUrl,
                                getString(RR.string.AA0368)
                            )
                        })
                    val dialog = CommonDialog.Builder(it)
                        .spannable(content)
                        .cancelTxt(getString(com.gw_reoqoo.resource.R.string.AA0011))
                        .sureTxt(getString(com.gw_reoqoo.resource.R.string.AA0010))
                        .build()
                    dialog.hideTitle()
                    dialog.setButtonListener(onSureClick = {
                        mFgViewModel.agreeTxtClick(true)
                        shareVM.districtLD.value?.let { entity ->
                            goToLogin(entity, account, pwd)
                        } ?: toast.show(RR.string.AA0017)
                    })
                    dialog.show()
                    return@setOnClickListener
                }
            }
            shareVM.districtLD.value?.let { entity ->
                goToLogin(entity, account, pwd)
            } ?: toast.show(RR.string.AA0017)
        }

        mViewBinding.etAccount.filters = arrayOf(mFilter)

        // 2.5s内连续点击5次则跳转到反馈页面
        mViewBinding.llLoginTop.setOnClickListener {
            checkFeedbackInfo()
        }
    }

    /**
     * 去登录
     * @param districtEntity DistrictEntity 地区实体
     * @param account String 账号
     * @param pwd String 密码
     */
    private fun goToLogin(entity: DistrictEntity, account: String, pwd: String) {
        // TODO: Please replace cbAutoLogin with your specific Auto Login control ID
//        mFgViewModel.setSelectAutoLogin(mViewBinding.cbAutoLogin.isChecked)

        mFgViewModel.loginByAccount(entity, account, pwd)
    }

    /**
     *  检查是否需要跳转到反馈页面
     */
    private fun checkFeedbackInfo() {
        mViewBinding.apply {
            val account = etAccount.text.toString().lowercase().trim()
            val password = etPwd.text.toString().lowercase().trim()
            if (account == REO_QOO && password == REO_QOO) {
                mFgViewModel.jumpToFeedback()
            }
        }
    }

    override fun onPreViewCreate() {
        super.onPreViewCreate()
        GwellLogUtils.i(TAG, "onPreViewCreate")
    }

    override fun initData() {
        super.initData()

        if (mFgViewModel.needShowProtocol()) {
            context?.let {
                val content =
                    PrivatePolicyStringKit.showProtocolDetail(it, false, showUserProtocol = {
                        val userProtocol = websiteApi.getUserProtocolUrl()
                        GwellLogUtils.i(TAG, "userProtocol $userProtocol")
                        webViewApi.openWebView(userProtocol, getString(RR.string.AA0044))
                    }, showPrivacyProtocol = {
                        val userPrivacyUrl = websiteApi.getUserPrivacyUrl()
                        GwellLogUtils.i(TAG, "userPrivacyUrl $userPrivacyUrl")
                        webViewApi.openWebView(userPrivacyUrl, getString(RR.string.AA0368))
                    })
                var isAgree = false
                val dialog = CommonDialog.Builder(it)
                    .title(getString(RR.string.AA0045))
                    .spannable(content)
                    .cancelVisibility(View.VISIBLE)
                    .sureTxt(getString(com.gw_reoqoo.resource.R.string.AA0010))
                    .cancelTxt(getString(com.gw_reoqoo.resource.R.string.AA0682))
                    .build()
                dialog.setButtonListener(onSureClick = {
                    isAgree = true
                    mFgViewModel.isAgreeProtocol(true)
                    gwIotOpt.updateAgreeProtocol()
                    initSaSDK()
                    requestNotificationPermissions()
                }, onCancelClick = {
                    mFgViewModel.isAgreeProtocol(false)
                    ActivityLifecycleManager.finishAllActivity()
                })
                dialog.setOnDismissListener {
                    if (!isAgree) {
                        ActivityLifecycleManager.finishAllActivity()
                    }
                }
                dialog.show()
            }
        } else {
            initSaSDK()
        }

        shareVM.initDistrictInfo(AppChannelName.isXiaotunApp(appParamApi.getAppName()))
    }

    /**
     * 跳转到账户输入页面
     * @param type AccountInputType  注册账号 和 忘记密码
     */
    private fun jumpToAccountInputFragment(type: AccountInputType) {
        shareVM.districtLD.value?.let {
            val action =
                LoginFragmentDirections.accountActionAccountLoginfragmentToAccountAccountinputfragment(
                    it,
                    type
                )
            findNavController().navigate(action)
        }
    }

    override fun initLiveData(viewModel: LoginFrgVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)
        mFgViewModel.isAgree.observe(this) {
            mViewBinding.imgAgree.isChecked = it
        }

        shareVM.districtLD.observe(viewLifecycleOwner) {
            Log.i(TAG, "districtLD codeEntity = $it")
            mViewBinding.tvArea.text = it.districtName
            val areaCode = buildString {
                append("+")
                append(it.districtCode)
            }
            mViewBinding.tvAreaId.text = areaCode
            // 获取 TextView 使用的 Paint 对象
            val textPaint: TextPaint = mViewBinding.tvAreaId.paint
            // 使用 Paint 测量文本的宽度
            val textWidth = textPaint.measureText(areaCode)
            GwellLogUtils.i(TAG, "etAccount.width = $textWidth")
            mViewBinding.etAccount.setPadding(textWidth.toInt() + 30.dp, 0, 13.dp, 0)
        }

        // 跳转到用户反馈页面
        mFgViewModel.startFeedbackLD.observe(this) {
            if (it) {
                TheRouter.build(ReoqooRouterPath.MinePath.ACTIVITY_FEEDBACK).navigation()
            }
        }
    }

    /**
     * 用户同意隐私政策概要后才去初始化神策SDK
     */
    private fun initSaSDK() {
        context?.let { context ->
            SA.init(
                context,
                false,
                LanguageUtils.getLanguage2(context),
                VersionUtils.getAppVersionName(context),
                SA.AppType.REOQOO
            )
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.account_fragment_login
    }

    override fun <T : ViewModel?> loadViewModel(): Class<T> {
        return LoginFrgVM::class.java as Class<T>
    }

    /**
     * Android SDK版本 33+需要动态请求通知栏权限
     */
    private fun requestNotificationPermissions() {
        // 检查 API 版本是否为 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GwellLogUtils.i(TAG, "requestNotificationPermissions")
            // 检查是否已获得通知权限
            context?.let {
                if (ContextCompat.checkSelfPermission(it, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // 请求通知栏权限, 自定义权限请求码 NOTIFICATION_PERMISSION_REQUEST_CODE
                    activity?.let { it1 ->
                        ActivityCompat.requestPermissions(
                            it1,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        GwellLogUtils.i(TAG, "onRequestPermissionsResult requestCode = $requestCode")
    }
}