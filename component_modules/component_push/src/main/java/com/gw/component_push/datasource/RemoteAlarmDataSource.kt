package com.gw.component_push.datasource

import com.gw.component_push.entity.AlarmInfoEntity
import com.gw.lib_http.RespResult
import com.gw.lib_http.ResponseNotSuccessException
import com.gw.lib_http.mapActionFlow
import com.gw.lib_http.typeSubscriber
import com.gw.lib_http.wrapper.HttpServiceWrapper
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import com.tencentcs.iotvideo.vas.VasMgr
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/8/7 16:52
 * Description: RemoteMsgDataSource
 */
class RemoteAlarmDataSource @Inject constructor(
    private val httpService: HttpServiceWrapper
) {

    companion object {
        private const val TAG = "RemoteAlarmDataSource"
    }

    private val vasService by lazy { VasMgr.getVasService() }

    /**
     * 获取消息列表数据
     *
     * @return RespResult<MsgListEntity> 回调
     */
    suspend fun getAlarmEventInfo(tid: String, alarmId: String): RespResult<AlarmInfoEntity> {
        val result = Channel<RespResult<AlarmInfoEntity>>(1)
        vasService.queryEventInfo(tid, alarmId,
            typeSubscriber<AlarmInfoEntity>(
                onSuccess = {
                    result.trySend(RespResult.Success(it))
                },
                onFail = {
                    if (it is ResponseNotSuccessException) {
                        result.trySend(RespResult.ServerError(it.code, it.msg))
                    } else {
                        result.trySend(RespResult.LocalError(it))
                    }
                }
            )
        )
        return result.receive()
    }

}