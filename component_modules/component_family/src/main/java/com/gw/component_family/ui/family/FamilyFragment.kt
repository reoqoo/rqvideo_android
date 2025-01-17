package com.gw.component_family.ui.family

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gw.component_device_share.api.DevShareApi.Companion.KEY_PARAM_PAGE_FROM
import com.gw.component_family.R
import com.gw.component_family.api.interfaces.IShareDeviceApi
import com.gw.component_family.api.interfaces.SaEvent.AddDevice.BUTTON_CLICK
import com.gw.component_family.databinding.FamilyDialogAddDevSuccessBinding
import com.gw.component_family.databinding.FamilyFragmentFamilyBinding
import com.gw.component_family.entrties.EventSysEntity
import com.gw.component_family.ui.family.adapter.FragmentAdapter
import com.gw.component_family.ui.family.adapter.TitleAdapter
import com.gw.component_family.ui.family.vm.FamilyVM
import com.gw.component_push.api.interfaces.IAlarmEventApi
import com.gw.cp_account.api.kapi.IAccountMgrApi
import com.gw.cp_config.api.IAppConfigApi
import com.gw.cp_config.api.ProductImgType
import com.gw.cp_msg.api.kapi.INoticeMgrApi
import com.gw.cp_msg.entity.http.MainNoticeEntity
import com.gw.lib_base_architecture.view.ABaseMVVMDBFragment
import com.gw.lib_http.ResponseNotSuccessException
import com.gw.lib_http.entities.MessageBean
import com.gw.lib_http.entities.ScanShareQRCodeResult
import com.gw.lib_http.error.ResponseCode
import com.gw.lib_iotvideo.EventTopicType
import com.gw.lib_iotvideo.IoTVideoMgr
import com.gw.lib_room.ktx.isMaster
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_router.navigation
import com.gw.lib_utils.ktx.launch
import com.gw.lib_utils.ktx.loadUrl
import com.gw.lib_utils.ktx.setSingleClickListener
import com.gw.lib_utils.ktx.visible
import com.gw.lib_widget.dialog.comm_dialog.entity.CommDialogAction
import com.gw.lib_widget.dialog.comm_dialog.entity.CustomContent
import com.gw.lib_widget.dialog.comm_dialog.entity.TextContent
import com.gw.lib_widget.dialog.comm_dialog.ext.showCommDialog
import com.gw.lib_widget.popups.CommListPopup
import com.gw.lib_widget.popups.GuidePopup
import com.gw.reoqoosdk.constant.NetConfigConstant
import com.gw.reoqoosdk.dev_monitor.IMonitorService
import com.gw.reoqoosdk.paid_service.IPaidService
import com.gw.widget_webview.jsinterface.WebViewJSCallbackImpl
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_statistics.sa.kits.SA
import com.jwkj.base_utils.activity_utils.ActivityUtils
import com.jwkj.base_utils.ui.DensityUtil
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import com.tencentcs.iotvideo.messagemgr.EventMessage
import com.tencentcs.iotvideo.messagemgr.IEventListener
import com.tencentcs.iotvideo.utils.JSONUtils
import com.therouter.router.Autowired
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.gw.resource.R as RR


/**
 * @Description: - 家庭界面
 * @Author: XIAOLEI
 * @Date: 2023/8/1
 */
@Route(path = ReoqooRouterPath.Family.FAMILY_FRAGMENT_PATH)
@AndroidEntryPoint
class FamilyFragment : ABaseMVVMDBFragment<FamilyFragmentFamilyBinding, FamilyVM>() {

    companion object {
        private const val TAG = "FamilyFragment"

        /**
         * 毛玻璃效果的模糊度
         */
        private const val BLUR_NUMBER = 25F
    }

    override fun getLayoutId() = R.layout.family_fragment_family
    override fun <T : ViewModel?> loadViewModel() = FamilyVM::class.java as Class<T>

    @Autowired
    lateinit var userId: String

    @Inject
    lateinit var iMonitorService: IMonitorService

    @Inject
    lateinit var configApi: IAppConfigApi

    @Inject
    lateinit var shareDeviceApi: IShareDeviceApi

    @Inject
    lateinit var accountMgrApi: IAccountMgrApi

    @Inject
    lateinit var iCloudService: IPaidService

    @Inject
    lateinit var noticeMgrApi: INoticeMgrApi

    @Inject
    lateinit var alarmEventApi: IAlarmEventApi

    private val fragmentAdapter by lazy { FragmentAdapter(this) }

    private val titleAdapter by lazy { TitleAdapter() }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        // 设置viewpager，并且监听viewpager的翻页改变事件
        mViewBinding.viewPager.run {
            adapter = fragmentAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val titleAdapter = mViewBinding.titleRv.adapter as? TitleAdapter?
                    titleAdapter?.setCurrent(position)
                }
            })
        }
        // 设置顶部标题
        mViewBinding.titleRv.run {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = titleAdapter
        }
        // 添加按钮
        mViewBinding.addBtn.setSingleClickListener { v ->
            val anyDeviceMaster = mFgViewModel.deviceList.value?.any { it.isMaster } == true
            val items = listOf(
                CommListPopup.CommItem(getString(RR.string.AA0049)) {
                    SA.track(BUTTON_CLICK)
                    ReoqooRouterPath
                        .ConfigPath
                        .CONFIG_SCAN_ACTIVITY_PATH
                        .navigation(fragment = null)
                },
                CommListPopup.CommItem(
                    getString(RR.string.AA0050),
                    enable = anyDeviceMaster
                ) {
                    ReoqooRouterPath
                        .DevShare
                        .ACTIVITY_SHARE_DEVICE
                        .navigation(
                            fragment = null,
                            with = mapOf(KEY_PARAM_PAGE_FROM to "MainActivity")
                        )
                },
                CommListPopup.CommItem(getString(RR.string.AA0051)) {
                    SA.track(BUTTON_CLICK)
                    ReoqooRouterPath
                        .ConfigPath
                        .CONFIG_SCAN_ACTIVITY_PATH
                        .navigation(
                            fragment = null,
                            params = mapOf(NetConfigConstant.KEY_ENTER_METHOD to NetConfigConstant.METHOD_SCAN)
                        )
                },
            )
            CommListPopup(v.context, items).showAsDropDown(v)
        }

        // 下拉刷新的回调
        mViewBinding.pullToRefreshLayout.onLoading { loadingHandler ->
            // 获取未读的设备分享消息列表
            mFgViewModel.loadUnRadDevShareList()
            // 刷新设备列表
            mFgViewModel.loadDeviceAndSceneList(this) {
                loadingHandler.cancelLoading()
            }
        }
        // 设备邀请弹窗 忽略
        mViewBinding.tvIgnore.setSingleClickListener {
            mFgViewModel.ignoreAndRestoreDevShareMessage()
        }
        // 设备邀请弹窗 查看
        mViewBinding.tvCheck.setSingleClickListener {
            val msg = it.tag
            if (msg !is MessageBean) return@setSingleClickListener
            val content = msg.getDeviceShareContent()
            if (content != null) {
                mFgViewModel.ignoreAndRestoreDevShareMessage()
                shareDeviceApi.showShareDetailDialog(this,
                    content.shareToken,
                    content.deviceId,
                    "",
                    onClickAccept = {
                        mFgViewModel.loadRemoteDeviceList()
                    }
                )
            }
        }

        mViewBinding.ivCloseFloatBanner.setSingleClickListener {
            mViewBinding.rlFloatBanner.visible(false)
        }
    }

    override fun initData() {
        super.initData()

        mFgViewModel.loadNoticeList()
    }

    override fun initLiveData(viewModel: FamilyVM, savedInstanceState: Bundle?) {
        super.initLiveData(viewModel, savedInstanceState)
        // 监听账户信息改动
        viewModel.watchAccountInfo.observe(this) { iUserInfo ->
            // 顶部XXX的家
            mViewBinding.tvHomeUser.text =
                getString(RR.string.AA0047, iUserInfo?.getInsensitiveName(true) ?: "")
        }
        // 监听fragment适配器数据的改变
        viewModel.adapterList.observe(this) { list ->
            fragmentAdapter.updateData(list)
            titleAdapter.setData(list, 0)
        }
        // 监听设备分享消息
        viewModel.unRadDevShareList.observe(this) { list ->
            GwellLogUtils.i(TAG, "unRadDevShareList: $list")
            mViewBinding.llDevShareLayout.isVisible = list.isNotEmpty()
            val first = list.firstOrNull { it.getDeviceShareContent() != null }
            mViewBinding.tvCheck.tag = first
            if (first != null) {
                val msgContent = first.getDeviceShareContent()
                val content = getString(RR.string.AA0161).format(msgContent?.inviteAccount)
                mViewBinding.tvDevShareTitle.text = content
            }
        }
        // 添加按钮的引导
        viewModel.addBtnGuide.observe(this) { shown ->
            if (!shown) {
                context?.let {
                    GuidePopup(it, this, RR.string.AA0336).apply {
                        setOnIKnowClick {
                            viewModel.iKnowAddBtnGuide()
                        }
                    }
                }?.show(mViewBinding.addBtn)
            }
        }
        // 当扫描成功，并且解析的livedata
        viewModel.onScanShareDevice.observe(this) { param ->
            GwellLogUtils.i(TAG, "onScanShareDevice: $param")
            launch {
                // 访客通过扫描请求结果
                viewModel.addDeviceByScanShareCode(param)?.collect { action ->
                    when (action) {
                        is HttpAction.Loading -> Unit
                        // 成功弹出添加成功的弹窗
                        is HttpAction.Success -> {
                            val data = action.data as? ScanShareQRCodeResult?
                            if (data != null) {
                                // 刷新远程数据
                                viewModel.loadRemoteDeviceList()
                                // 弹窗
                                val binding =
                                    FamilyDialogAddDevSuccessBinding.inflate(layoutInflater)
                                showCommDialog {
                                    content = CustomContent(
                                        binding,
                                        initView = { binding ->
                                            binding.tvProductName.text =
                                                configApi.getProductName("${data.pid}")
                                            val imgUrl = configApi.getProductImgUrl(
                                                "${data.pid}",
                                                imgType = ProductImgType.INTRODUCTION
                                            )
                                            binding.ivProductImg.loadUrl(imgUrl)
                                        }
                                    )
                                    actions = listOf(
                                        CommDialogAction(getString(RR.string.AA0059)),
                                        CommDialogAction(
                                            text = getString(RR.string.AA0166),
                                            onClick = {
                                                iMonitorService.startMonitorActivity(data.devId)
                                            }
                                        ),
                                    )
                                }
                            }
                        }
                        // 失败分情况弹窗
                        is HttpAction.Fail -> {
                            when (val error = action.t) {
                                is ResponseNotSuccessException -> {
                                    when (val respCode = ResponseCode.getRespCode(error.code)) {
                                        null -> Unit

                                        ResponseCode.CODE_11048,
                                        ResponseCode.CODE_10905009,
                                        ResponseCode.CODE_11044 -> {
                                            showCommDialog {
                                                content = TextContent(getString(respCode.msgRes))
                                                actions = listOf(
                                                    CommDialogAction(getString(RR.string.AA0334)),
                                                )
                                            }
                                        }

                                        else -> {
                                            toast.show(respCode.msgRes)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.mainNoticeEntity.observe(this) { notice ->
            val context = this.requireActivity()
            if (ActivityUtils.isActivityUsable(context)) {
                iCloudService.openWebViewDialog(
                    activity = context,
                    width = DensityUtil.getScreenWidth(context),
                    height = DensityUtil.getScreenHeight(context),
                    url = notice.url,
                    deviceId = notice.deviceId,
                    callBack = object : WebViewJSCallbackImpl() {
                        /**
                         * 打开其他webview
                         *
                         * @param url webview地址
                         */
                        override fun openWebView(url: String?) {
                            super.openWebView(url)
                            if (url.isNullOrEmpty()) {
                                GwellLogUtils.e(TAG, "openWebView: url is null or empty")
                                return
                            }
                            iCloudService.openWebView(
                                url = url,
                                title = "",
                                deviceId = notice.deviceId
                            )
                        }

                        override fun dialogShow() {
                            super.dialogShow()
                            GwellLogUtils.i(TAG, "dialogShow: notice $notice")
                            noticeMgrApi.deleteMainNotice(notice)
                            launch(Dispatchers.IO) {
                                if (notice.type == MainNoticeEntity.Type.USER_MSG) {
                                    notice.msgId?.let { noticeMgrApi.setUserMessageState(it, 1) }
                                } else if (notice.type == MainNoticeEntity.Type.SYSTEM_NOTICE) {
                                    notice.tag?.let { noticeMgrApi.setNoticeState(it, 1) }
                                }
                            }
                        }

                        override fun dialogDismiss() {
                            super.dialogDismiss()
                            notice.isHaveShow = true
                        }
                    }
                )
            }
        }

        viewModel.floatBannerEntity.observe(this) { banner ->
            mViewBinding.rlFloatBanner.visible(true)
            mViewBinding.ivFloatBanner.loadUrl(banner.picUrl)
            mViewBinding.ivFloatBanner.setOnClickListener {
                GwellLogUtils.i(TAG, "banner.url-${banner.url}")
                banner?.url?.let {
                    iCloudService.openWebView(it, "")
                }
            }
        }

        viewModel.homeBannerEntity.observe(this) { banner ->
            mViewBinding.ivHomeBanner.loadUrl(banner.picUrl)
            mViewBinding.ivHomeBanner.setOnClickListener {
                GwellLogUtils.i(TAG, "banner.url-${banner.url}")
                banner?.url?.let {
                    iCloudService.openWebView(it, "")
                }
            }
            mViewBinding.ivHomeBannerClose.setOnClickListener {
                mViewBinding.rlHomeBanner.visible(false)
            }
            if (!viewModel.deviceList.value.isNullOrEmpty()) {
                mViewBinding.rlHomeBanner.visible(true)
            }
        }

        viewModel.deviceList.observe(this) {
            if (it.isNullOrEmpty()) {
                mViewBinding.rlHomeBanner.visible(false)
            } else {
                mViewBinding.rlHomeBanner.visible(viewModel.homeBannerEntity.value != null)
            }
        }

        // 设备分享p2p消息
        IoTVideoMgr.addEventListener(eventListener)
    }

    /**
     * 事件监听
     */
    private val eventListener = IEventListener { event: EventMessage ->
        GwellLogUtils.i(TAG, "IoTVideoMgr.eventMessage.observe-$event")
        when (event.topic) {
            EventTopicType.NOTIFY_ALARM,
            EventTopicType.NOTIFY_ALARM_MANUAL,
            EventTopicType.NOTIFY_ALARM_TRIGGER_V2,
            EventTopicType.NOTIFY_ALARM_CHANGE,
            EventTopicType.NOTIFY_ALARM_CHANGE_V2 -> {
                // 设备告警
                alarmEventApi.analyticAlarmEvent(event)
            }

            EventTopicType.NOTIFY_USER_MSG_UPDATE -> {
                // 更新用户消息的消息
                // TODO 这个地方需要更新用户消息和公告消息，逻辑暂时改，后续优化
                mFgViewModel.loadUnRadDevShareList()
                launch(Dispatchers.IO) {
                    noticeMgrApi.requestMsg(true)
                }
            }

            EventTopicType.NOTIFY_USER_PWD_CHANGE -> {
                accountMgrApi.logout()
            }

            EventTopicType.NOTIFY_PUSH -> {
                // TODO 消息事件（新版本事件走push_msg的type，为了兼容X10B设备老版本，所以trig也不改动）
                // 设备告警
                alarmEventApi.analyticAlarmEvent(event)
            }

            EventTopicType.NOTIFY_ONLINE -> {}
            EventTopicType.NOTIFY_UNBOUND -> {
                GwellLogUtils.i(TAG, "NOTIFY_UNBOUND")
                val eventSysEntity = JSONUtils.JsonToEntity(event.data, EventSysEntity::class.java)
                GwellLogUtils.i(TAG, "eventSysEntity: $eventSysEntity")
                eventSysEntity?.let {
                    if (it.topic?.contains("unbindGuest") == true) {
                        iMonitorService.onDeviceCancelShare(eventSysEntity.data.did)
                    } else if (it.topic?.contains("unbindOwner") == true) {
                        iMonitorService.deviceDelByOwner(eventSysEntity.data.did)
                        mFgViewModel.loadDeviceAndSceneList(this)
                    }
                }
            }

            EventTopicType.NOTIFY_GUEST_DELETED_BY_MASTER -> {}
            EventTopicType.NOTIFY_DEV_PERMISSION_UPDATE -> {}
            EventTopicType.NOTIFY_MESSAGE_CENTER_UPDATE -> {

            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        GwellLogUtils.i(TAG, "onHiddenChanged-$hidden")
        if (!hidden) {
            // 加载设备和场景列表
            mFgViewModel.loadDeviceAndSceneList(this)
            // 获取未读的设备分享消息列表
            mFgViewModel.loadUnRadDevShareList()
        }
    }

    override fun onResume() {
        super.onResume()
        GwellLogUtils.i(TAG, "onResume")
        // 加载设备和场景列表
        mFgViewModel.loadDeviceAndSceneList(this)
        // 获取未读的设备分享消息列表
        mFgViewModel.loadUnRadDevShareList()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 设备分享p2p消息
        IoTVideoMgr.removeEventListener(eventListener)
    }
}