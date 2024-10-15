package com.gw.component_family.ui.device_empty

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import com.gw.component_family.R
import com.gw.component_family.api.interfaces.SaEvent.AddDevice.BUTTON_CLICK
import com.gw.component_family.databinding.FamilyFragmentDeviceEmptyBinding
import com.gw.component_family.ui.device_empty.vm.EmptyDeviceVM
import com.gw.lib_base_architecture.view.ABaseMVVMDBFragment
import com.gw.lib_router.ReoqooRouterPath
import com.gw.lib_router.navigation
import com.jwkj.base_statistics.sa.kits.SA
import com.therouter.router.Autowired
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint

/**
 * @Description: - 当设备列表为空的时候，则以此页面替换
 * @Author: XIAOLEI
 * @Date: 2023/8/1
 */
@Route(path = ReoqooRouterPath.Family.FAMILY_FRAGMENT_DEVICE_EMPTY_PATH)
@AndroidEntryPoint
class EmptyDeviceFragment : ABaseMVVMDBFragment<FamilyFragmentDeviceEmptyBinding, EmptyDeviceVM>() {

    @Autowired
    lateinit var userId: String


    override fun getLayoutId() = R.layout.family_fragment_device_empty

    override fun <T : ViewModel?> loadViewModel() = EmptyDeviceVM::class.java as Class<T>

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        // 点击添加假数据
        mViewBinding.addBtn.setOnClickListener {
            SA.track(BUTTON_CLICK)
            ReoqooRouterPath
                .ConfigPath
                .CONFIG_SCAN_ACTIVITY_PATH
                .navigation(fragment = null)
        }
    }
}