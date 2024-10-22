package com.gw.cp_account.ui.fragment.login

import android.os.Bundle
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.gw.cp_account.R
import com.gw.cp_account.databinding.AccountFragmentLoginBinding
import com.gw.cp_account.entity.AccountInputType
import com.gw.cp_account.kits.PrivatePolicyStringKit
import com.gw.cp_account.sa.AccountSaEvent
import com.gw.cp_account.ui.fragment.ShareVM
import com.gw.lib_base_architecture.ToastIntentData
import com.gw.lib_base_architecture.view.ABaseMVVMDBFragment
import com.gw.lib_utils.ktx.dp
import com.gw.lib_widget.dialog.CommonDialog
import com.gw.reoqoosdk.paid_service.IPaidService
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_lifecycle.activity_lifecycle.ActivityLifecycleManager
import com.jwkj.base_statistics.sa.kits.SA
import com.jwkj.base_utils.local.LanguageUtils
import com.jwkj.base_utils.str_utils.GwStringUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.gw.resource.R as RR

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/8/1 16:22
 * Description: 登录页
 */
@AndroidEntryPoint
class LoginFragment : ABaseMVVMDBFragment<AccountFragmentLoginBinding, LoginFrgVM>() {

    companion object {
        private const val TAG = "LoginFragment"

        val address = "https://www.google.com/"
    }

    @Inject
    lateinit var iCloudService: IPaidService

    /**
     * 共享VM，主要处理数据回传的问题
     */
    private val shareVM: ShareVM by activityViewModels()

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)

        mViewBinding.tvArea.setOnClickListener {
            val action =
                LoginFragmentDirections.accountActionAccountLoginfragmentToAccountArealistfragment(
                    shareVM.districtLD.value
                )
            findNavController().navigate(action)
        }

        mViewBinding.etPwd.setOnFocusChangeListener { v, hasFocus ->
            mViewBinding.ctlPwd.isSelected = hasFocus
        }

        mViewBinding.tvForgetPwd.setOnClickListener {
            SA.track(AccountSaEvent.LOGIN_FORGOTPASSWORDBUTTONCLICK)
            shareVM.districtLD.value?.let {
                val action =
                    LoginFragmentDirections.accountActionAccountLoginfragmentToAccountAccountinputfragment(
                        it,
                        AccountInputType.ACCOUNT_FORGET
                    )
                findNavController().navigate(action)
            }
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
            val action =
                LoginFragmentDirections.accountActionAccountLoginfragmentToAccountRegisterareafragment(
                    shareVM.districtLD.value
                )
            findNavController().navigate(action)
        }

        mViewBinding.tvAgree.movementMethod = LinkMovementMethod.getInstance()
        mViewBinding.tvAgree.text =
            PrivatePolicyStringKit.getAgreement(requireActivity(), false, showUserProtocol = {
                iCloudService.openWebView(address, getString(RR.string.AA0044))
            }, showPrivacyProtocol = {
                iCloudService.openWebView(address, getString(RR.string.AA0368))
            })

        mViewBinding.imgAgree.setOnCheckedChangeListener { _, isChecked ->
            mFgViewModel.agreeBtnClick(isChecked)
        }

        mViewBinding.btnLogin.setOnClickListener {
            val account = mViewBinding.etAccount.text.toString()
            GwellLogUtils.i(TAG, "account = $account")
            val pwd = mViewBinding.etPwd.text.toString()
            GwellLogUtils.i(TAG, "pwd = $pwd")
            if (account.isEmpty()) {
                toast.show(RR.string.AA0578)
                return@setOnClickListener
            }
            if (pwd.isEmpty()) {
                toast.show(RR.string.AA0579)
                return@setOnClickListener
            }
            if (!GwStringUtils.isMobileNO(account) &&
                !GwStringUtils.isEmail(account)
            ) {
                toast.show(RR.string.AA0012)
                return@setOnClickListener
            }
            if (null == shareVM.districtLD.value) {
                mFgViewModel.toastIntentData.postValue(ToastIntentData(com.gw.resource.R.string.AA0017))
                GwellLogUtils.e(TAG, "地区信息初始化失败")
                return@setOnClickListener
            }
            GwellLogUtils.i(TAG, "isAgree = ${mFgViewModel.isAgree.value}")
            if (mFgViewModel.isAgree.value == false) {
                context?.let {
                    val content = PrivatePolicyStringKit.getAgreement(
                        it,
                        false,
                        showUserProtocol = {
                            iCloudService.openWebView(address, getString(RR.string.AA0044))
                        },
                        showPrivacyProtocol = {
                            iCloudService.openWebView(address, getString(RR.string.AA0368))
                        })
                    val dialog = CommonDialog.Builder(it)
                        .spannable(content)
                        .cancelTxt(getString(com.gw.resource.R.string.AA0011))
                        .sureTxt(getString(com.gw.resource.R.string.AA0010))
                        .build()
                    dialog.hideTitle()
                    dialog.setButtonListener(onSureClick = {
                        mFgViewModel.agreeTxtClick(true)
                        shareVM.districtLD.value?.let { entity ->
                            mFgViewModel.loginByAccount(entity, account, pwd)
                        } ?: toast.show(RR.string.AA0017)
                    })
                    dialog.show()
                    return@setOnClickListener
                }
            }
            shareVM.districtLD.value?.let {
                mFgViewModel.loginByAccount(it, account, pwd)
            } ?: toast.show(RR.string.AA0017)
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
                        iCloudService.openWebView(address, getString(RR.string.AA0044))
                    }, showPrivacyProtocol = {
                        iCloudService.openWebView(address, getString(RR.string.AA0368))
                    })
                var isAgree = false
                val dialog = CommonDialog.Builder(it)
                    .title(getString(RR.string.AA0045))
                    .spannable(content)
                    .cancelVisibility(View.GONE)
                    .sureTxt(getString(com.gw.resource.R.string.AA0010))
                    .build()
                dialog.setButtonListener(onSureClick = {
                    isAgree = true
                    mFgViewModel.isAgreeProtocol(true)
                })
                dialog.setOnDismissListener {
                    if (!isAgree) {
                        ActivityLifecycleManager.finishAllActivity()
                    }
                }
                dialog.show()
            }
        }

        shareVM.initDistrictInfo()
    }

    override fun initLiveData(viewModel: LoginFrgVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)
        mFgViewModel.isAgree.observe(this) {
            context?.let { context ->
                SA.init(
                    context,
                    false,
                    LanguageUtils.getLanguage2(context),
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName,
                    SA.AppType.REOQOO
                )
            }
            mViewBinding.imgAgree.isChecked = it
        }

        shareVM.districtLD.observe(viewLifecycleOwner) {
            GwellLogUtils.i(TAG, "codeEntity = $it")
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
    }

    override fun getLayoutId(): Int {
        return R.layout.account_fragment_login
    }

    override fun <T : ViewModel?> loadViewModel(): Class<T> {
        return LoginFrgVM::class.java as Class<T>
    }

}