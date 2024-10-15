package com.gw.component_family.ui.device_list.vm

import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gw.component_family.api.interfaces.IGuideDataStore
import com.gw.component_family.repository.DeviceRepository
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gw.lib_room.device.DeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @Description: - 设备列表界面ViewModel
 * @Author: XIAOLEI
 * @Date: 2023/8/1
 *
 * @param deviceRepository 设备的Repository
 * @param guideDataStore 新手引导的数据存储
 */
@HiltViewModel
class DeviceListVM @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val guideDataStore: IGuideDataStore,
) : ABaseVM() {
    
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 第一个设备的新手引导
     */
    private val _firstDeviceGuide = MutableLiveData<Boolean?>()
    
    /**
     * 第一个设备的新手引导
     */
    val firstDeviceGuide: LiveData<Boolean?> get() = _firstDeviceGuide
    
    /**
     * 引导是否显示
     */
    var isDeviceGuidShow = false
    
    /**
     * 更新设备信息
     */
    fun updateDevice(list: List<DeviceInfo>) {
        deviceRepository.updateDevice(list)
    }
    
    /**
     * 根据设备ID，查询设备是否开启云服务
     * @param deviceId 设备ID
     */
    fun checkDeviceCloudOn(deviceId: String): Boolean? {
        return deviceRepository.checkDeviceCloudOn(deviceId)
    }
    
    /**
     * 加载第一个设备的新手引导
     */
    fun loadFirstDeviceGuide() {
        viewModelScope.launch {
            val shown = guideDataStore.getFirstDeviceGuide()
            handler.removeCallbacksAndMessages(null)
            if (!shown) {
                handler.postDelayed(500) {
                    _firstDeviceGuide.postValue(false)
                }
            }
        }
    }
    
    /**
     * 第一个设备的新手引导，点击我知道了
     */
    fun iKnowFirstDeviceGuide() {
        viewModelScope.launch {
            guideDataStore.setFirstDeviceGuide(true)
            _firstDeviceGuide.postValue(null)
        }
    }
    
    override fun onCleared() {
        handler.removeCallbacksAndMessages(null)
    }
}