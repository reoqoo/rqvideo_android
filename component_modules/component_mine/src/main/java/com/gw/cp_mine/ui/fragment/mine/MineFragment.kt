package com.gw.cp_mine.ui.fragment.mine

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.gw.cp_mine.R
import com.gw.cp_mine.databinding.MineFragmentMineBinding
import com.gw.cp_mine.entity.MenuListEntity
import com.gw.cp_mine.ui.fragment.mine.adapter.MenuListAdapter
import com.gw.cp_mine.ui.fragment.mine.vm.MineFgVM
import com.gw.lib_base_architecture.view.ABaseMVVMDBFragment
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_utils.ktx.loadUrl
import com.gw.lib_utils.ktx.setSingleClickListener
import com.gw.lib_utils.ktx.visible
import com.gw.reoqoosdk.dev_upgrade.IDevUpgradeService
import com.gw.reoqoosdk.paid_service.IPaidService
import com.gw.reoqoosdk.setting_service.ISettingService
import com.gwell.loglibs.GwellLogUtils
import com.therouter.router.Route
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.gw.resource.R as RR

/**
@author: xuhaoyuan
@date: 2023/8/18
description:
1.我的页面
 */
@AndroidEntryPoint
@Route(path = ReoqooRouterPath.MinePath.FRAGMENT_MAIN)
class MineFragment : ABaseMVVMDBFragment<MineFragmentMineBinding, MineFgVM>() {

    companion object {
        private const val TAG = "MineFragment"
    }

    private var mAdapter: MenuListAdapter? = null

    @Inject
    lateinit var iCloudService: IPaidService

    @Inject
    lateinit var iSettingService: ISettingService

    @Inject
    lateinit var upgradeApi: IDevUpgradeService

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)

        mViewBinding.ivMessage.addStatusBarTopPadding()

        mViewBinding.ivMessage.setSingleClickListener {
            mFgViewModel.jumpToNext(ReoqooRouterPath.MsgCenterPath.ACTIVITY_MSG_CENTER)
        }
        mViewBinding.clUserMsg.setSingleClickListener {
            mFgViewModel.jumpToNext(ReoqooRouterPath.AccountPath.ACTIVITY_USER_INFO)
        }
        mViewBinding.ivSetting.setSingleClickListener {
            mFgViewModel.jumpToNext(ReoqooRouterPath.MinePath.ACTIVITY_SETTINGS)
        }
        mViewBinding.btnDevUpgrade.setSingleClickListener {
            mFgViewModel.jumpToNext(ReoqooRouterPath.Family.FAMILY_ACTIVITY_DEVICE_UPDATE)
        }
        mViewBinding.btnAlbum.setSingleClickListener {
            mFgViewModel.startAlbumPage()
        }
        mViewBinding.btnShare.setSingleClickListener {
            mFgViewModel.jumpToNext(ReoqooRouterPath.DevShare.ACTIVITY_SHARE_MANAGER_PATH)
        }
        mViewBinding.btnCloud.setSingleClickListener {
            iCloudService.offerCloudService()
        }
        mViewBinding.btn4g.setSingleClickListener {
            iCloudService.offer4GService()
        }
        mViewBinding.rvMenuList.layoutManager = LinearLayoutManager(context)
        mAdapter = MenuListAdapter().also {
            it.setOnItemClickListener(object : MenuListAdapter.ItemClickListener {
                override fun onItemClick(item: MenuListEntity) {
                    item.routerPath.let { _path ->
                        if (_path.isNotEmpty()) {
                            if (_path == ReoqooRouterPath.MinePath.ACTIVITY_FEEDBACK) {
                                iSettingService.goFeedbackPage(null, -1)
                                return
                            }
                            mFgViewModel.jumpToNext(_path)
                            GwellLogUtils.i(TAG, "MenuListAdapter: start jump")
                        }
                    }
                }
            })
        }
        mViewBinding.rvMenuList.adapter = mAdapter
    }

    override fun initData() {
        super.initData()
        mAdapter?.updateData(mFgViewModel.initLMenuList())

        mFgViewModel.updateUserInfo()
        mFgViewModel.checkAreaCloudServer()
        mFgViewModel.checkHas4GDevice()
        mFgViewModel.initUpgradeInfo()
    }

    override fun onResume() {
        super.onResume()
        mFgViewModel.updateMsgRedPoint()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        GwellLogUtils.i(TAG, "onHiddenChanged $hidden")
        if (!hidden) {
            mFgViewModel.initUpgradeInfo()
        }
    }

    override fun initLiveData(viewModel: MineFgVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)

        viewModel.watchUserInfo(this)

        viewModel.userInfo.observe(this) {
            it?.run {
                GwellLogUtils.i(TAG, "userInfo: ${this.userId}, ${this.nickName}, ${this.headUrl}")
                mViewBinding.tvUserName.text = this.getInsensitiveName()
                mViewBinding.ivHead.loadUrl(
                    imageUrl = headUrl,
                    placeHolder = RR.drawable.icon_default_avatar,
                    error = RR.drawable.icon_default_avatar,
                )
            }
        }

        upgradeApi.hasNewVersionDevice().observe(this) {
            mViewBinding.btnDevUpgrade.setPointVisible(it)
        }

        mFgViewModel.redPointState.observe(this) {
            mViewBinding.viewRedPoint.visible(it)
        }

        mFgViewModel.isSupportCloud.observe(this) {
            if (!it) {
                mViewBinding.btnCloud.visible(false)
                if (mFgViewModel.isSupport4G.value == false) {
                    mViewBinding.clEquityServices.visible(false)
                }
            } else {
                mViewBinding.btnCloud.visible(true)
                mViewBinding.clEquityServices.visible(true)
                mViewBinding.btn4g.visible(mFgViewModel.isSupport4G.value ?: false)
            }
        }
        mFgViewModel.isSupport4G.observe(this) {
            if (!it) {
                mViewBinding.btn4g.visible(false)
                if (mFgViewModel.isSupportCloud.value == false) {
                    mViewBinding.clEquityServices.visible(false)
                }
            } else {
                mViewBinding.btn4g.visible(true)
                mViewBinding.clEquityServices.visible(true)
                mViewBinding.btnCloud.visible(mFgViewModel.isSupportCloud.value ?: false)
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.mine_fragment_mine

    override fun <T : ViewModel?> loadViewModel(): Class<T> = MineFgVM::class.java as Class<T>

}