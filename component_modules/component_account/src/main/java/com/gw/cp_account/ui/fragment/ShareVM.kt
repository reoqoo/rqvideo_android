package com.gw.cp_account.ui.fragment

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gw.cp_account.datastore.AccountDataStoreApi
import com.gw.lib_http.entities.DistrictEntity
import com.gw.cp_account.utils.DistrictCodeUtils
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gwell.loglibs.GwellLogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/8/4 0:00
 * Description: 账号模块使用到的共享VM
 */
@HiltViewModel
class ShareVM @Inject constructor(private val api: AccountDataStoreApi) : ABaseVM() {

    companion object {
        private const val TAG = "ShareVM"
    }

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var districtCodeUtils: DistrictCodeUtils

    /**
     * 处理地区选择的结果回调
     */
    val districtLD = MutableLiveData<DistrictEntity>()

    /**
     * 初始化地区数据，默认为手机当前的地区
     */
    fun initDistrictInfo() {
        GwellLogUtils.i(TAG, "initDistrictInfo")
        viewModelScope.launch(Dispatchers.IO) {
            val entity = api.getUserDistrictEntity()
            entity?.let {
                GwellLogUtils.i(TAG, "entity = $entity")
                districtLD.postValue(it)
            } ?: let {
                districtCodeUtils.getCurrentDistrictCodeInfo()?.let {
                    GwellLogUtils.i(TAG, "getCurrentDistrictCodeInfo = $it")
                    districtLD.postValue(it)
                    api.setUserDistrictEntity(it)
                }
            }
        }
    }

    /**
     * 更新所选地区的信息
     *
     * @param entity DistrictEntity 地区信息
     */
    fun updateDistrict(entity: DistrictEntity) {
        districtLD.postValue(entity)
    }

    fun finishActivity() {
        finishActivityLD.postValue(true)
    }

}