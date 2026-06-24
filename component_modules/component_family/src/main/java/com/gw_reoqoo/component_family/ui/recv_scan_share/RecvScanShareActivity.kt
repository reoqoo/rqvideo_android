package com.gw_reoqoo.component_family.ui.recv_scan_share

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.gw_reoqoo.component_family.R
import com.gw_reoqoo.component_family.databinding.FamilyDialogAddDevSuccessBinding
import com.gw_reoqoo.lib_base_architecture.view.ABaseMVVMDBActivity
import com.gw_reoqoo.lib_http.ResponseNotSuccessException
import com.gw_reoqoo.lib_http.error.ResponseCode
import com.gw_reoqoo.lib_router.ReoqooRouterPath
import com.gw_reoqoo.lib_widget.dialog.comm_dialog.entity.CommDialogAction
import com.gw_reoqoo.lib_widget.dialog.comm_dialog.entity.TextContent
import com.gw_reoqoo.lib_widget.dialog.comm_dialog.ext.showCommDialog
import com.gwell.loglibs.GwellLogUtils
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Route(path = ReoqooRouterPath.Family.FAMILY_ACTIVITY_RECV_SCAN_SHARE)
class RecvScanShareActivity :
    ABaseMVVMDBActivity<FamilyDialogAddDevSuccessBinding, RecvScanShareVM>() {

    companion object {
        private const val TAG = "RecvScanShareActivity"
    }

    override fun getLayoutId(): Int {
        return R.layout.family_dialog_add_dev_success
    }

    override fun <T : ViewModel?> loadViewModel(): Class<T> {
        return RecvScanShareVM::class.java as Class<T>
    }

    override fun onContentViewLoad(savedInstanceState: Bundle?) {
        super.onContentViewLoad(savedInstanceState)
        window.decorView.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewLoadFinish() {
        super.onViewLoadFinish()
        setStatusBarColor()
    }

    override fun initView() {

        mViewBinding.activityContainer.setBackgroundResource(com.gw_reoqoo.resource.R.color.transparent)

        mViewBinding.ivProductImg.setImageResource(R.drawable.family_icon_device_holder_place)
        mViewBinding.llContentRoot.visibility = View.INVISIBLE

        mViewBinding.tvCancel.setOnClickListener {
            finish()
        }
        mViewBinding.tvAccept.setOnClickListener {
            mViewModel.openHome()
        }
    }

    override fun initLiveData(viewModel: RecvScanShareVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)
        (intent.getSerializableExtra("shareData") as? HashMap<String, String>)?.let {
            viewModel.requestShare(it)
        }
        viewModel.showCommDialogLD.observe(this) {
            GwellLogUtils.i(TAG, "showCommDialogLD = $it")
            mViewBinding.llContentRoot.visibility = View.GONE
            if (it == true) {
                showCommDialog {
                    content = TextContent(getString(com.gw_reoqoo.resource.R.string.AA0168))
                    actions = listOf(
                        CommDialogAction(getString(com.gw_reoqoo.resource.R.string.AA0131), { finish() })
                    )
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanShareFlow.collect { action ->
                    when (action) {
                        is HttpAction.Success -> {
                            GwellLogUtils.i(TAG, "scanShareData Success = ${action.data}")
                            dismissLoadDialog()
                            mViewBinding.llContentRoot.visibility = View.VISIBLE
                            mViewBinding.activityContainer.setBackgroundResource(com.gw_reoqoo.resource.R.color.black_20)
                            mViewBinding.tvProductName.text = action.data?.remarkName
                            viewModel.getProductImgWithPID(
                                action.data?.pid.toString(),
                                action.data?.productModel
                            )?.let {
                                Glide.with(this@RecvScanShareActivity)
                                    .load(it)
                                    .override(Target.SIZE_ORIGINAL)
                                    .error(R.drawable.family_icon_device_holder_place)
                                    .into(mViewBinding.ivProductImg)
                            }
                        }

                        is HttpAction.Fail -> {
                            when (val error = action.t) {
                                is ResponseNotSuccessException -> {
                                    when (val respCode = ResponseCode.getRespCode(error.code)) {
                                        null -> Unit
                                        ResponseCode.CODE_11048,
                                        ResponseCode.CODE_10905009,
                                        ResponseCode.CODE_11044 -> {
                                            toast.show(respCode.msgRes)
                                        }

                                        else -> {
                                            toast.show(respCode.msgRes)
                                        }
                                    }
                                }
                            }
                            finish()
                        }

                        is HttpAction.Loading -> {
                        }
                    }
                }
            }
        }
    }
}