package com.gw.cp_mine.ui.activity.about

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.gw.cp_mine.R
import com.gw.cp_mine.api.kapi.IMineModuleApi
import com.gw.cp_mine.databinding.MineActivityAboutBinding
import com.gw.lib_base_architecture.view.ABaseMVVMDBActivity
import com.gw.lib_router.ReoqooRouterPath
import com.gw.reoqoosdk.paid_service.IPaidService
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.gw.resource.R as RR

@AndroidEntryPoint
@Route(path = ReoqooRouterPath.MinePath.ACTIVITY_ABOUT)
class AboutActivity : ABaseMVVMDBActivity<MineActivityAboutBinding, AboutVM>() {

    companion object {
        private const val TAG = "AboutActivity"

        private const val address = "https://www.google.com/"

    }

    @Inject
    lateinit var iCloudService: IPaidService

    @Inject
    lateinit var mineApi: IMineModuleApi

    private var mAdapter: AboutListAdapter? = null

    override fun onViewLoadFinish() {
        setStatusBarColor()
    }

    override fun getLayoutId(): Int = R.layout.mine_activity_about

    override fun initView() {
        mViewBinding.appTitle.leftIcon.setOnClickListener { finish() }
        mViewBinding.tvAppName.text = getString(RR.string.AA0447)
        mViewBinding.tvAppVersion.text = buildString {
            append(getString(RR.string.AA0537))
            append(" ")
            append(packageManager.getPackageInfo(packageName, 0).versionName)
        }
        mViewBinding.rvAbout.layoutManager = LinearLayoutManager(applicationContext)
        mAdapter = AboutListAdapter(mViewModel.aboutMenuList).also {
            it.setOnItemClickListener { item ->
                when (item) {
                    AboutEnum.VERSION_UPDATE -> {
                        mineApi.appVersionUpgrade(this)
                    }

                    AboutEnum.USER_PROTOCOL -> {
                        iCloudService.openWebView(
                            address,
                            getString(item.strRes)
                        )
                    }

                    AboutEnum.PRIVACY_POLICY -> {
                        iCloudService.openWebView(
                            address,
                            getString(item.strRes)
                        )
                    }
                }
            }
        }
        mViewBinding.rvAbout.adapter = mAdapter
    }

    override fun <T : ViewModel?> loadViewModel(): Class<T> {
        return AboutVM::class.java as Class<T>
    }

    override fun initLiveData(viewModel: AboutVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)
    }

}