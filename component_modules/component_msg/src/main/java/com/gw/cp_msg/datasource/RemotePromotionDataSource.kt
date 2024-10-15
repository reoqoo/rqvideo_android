package com.gw.cp_msg.datasource

import com.gw.cp_msg.entity.http.PromotionListEntity
import com.gw.lib_http.mapActionFlow
import com.gw.lib_http.wrapper.HttpServiceWrapper
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/12/12 10:28
 * Description: RemotePromotionDataSource
 */
class RemotePromotionDataSource @Inject constructor(
    private val httpService: HttpServiceWrapper
) {

    /**
     * 获取运营数据
     *
     * @param devIds 设备ID
     * @return Flow<HttpAction<PromotionListEntity>>
     */
    fun getPromotionList(devIds: List<Long>): Flow<HttpAction<PromotionListEntity>> {
        return httpService.proFloatList(devIds).mapActionFlow()
    }

}