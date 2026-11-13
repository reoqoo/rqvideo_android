package com.gw.cp_msg.manger

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.gw.cp_msg.api.interfaces.ILocalMsgApi
import com.gw.cp_msg.api.kapi.IMsgExternalApi
import com.gw.cp_msg.datastore.IMsgDataStoreApi
import com.gw.cp_msg.entity.http.MsgDetailEntity
import com.gw.cp_msg.entity.http.MsgInfoListEntity
import com.gw_reoqoo.lib_http.entities.VersionInfoEntity
import com.gw.cp_msg.repository.MsgCenterRepository
import com.gw_reoqoo.lib_http.RespResult.LocalError
import com.gw_reoqoo.lib_http.RespResult.ServerError
import com.gw_reoqoo.lib_http.RespResult.Success
import com.gw_reoqoo.lib_http.entities.AppUpgradeEntity
import com.gw_reoqoo.lib_utils.version.Version
import com.gw_reoqoo.lib_utils.version.VersionUtils
import com.gw_reoqoo.resource.R
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_utils.str_utils.GwStringUtils
import com.reoqoo.component_iotapi_plugin_opt.api.IGWIotOpt
import com.therouter.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/11/2 11:16
 * Description: MsgManager
 */
@Singleton
class LocalMsgExternalManager @Inject constructor(
    private val app: Application,
    private val msgDataStore: IMsgDataStoreApi,
    private val msgCenterRepository: MsgCenterRepository,
    private val gwiotOpt: IGWIotOpt
) : ILocalMsgApi, IMsgExternalApi {

    companion object {
        private const val TAG = "MsgManager"
    }

    private val scope = MainScope()

    /**
     * 串行化加载锁，避免并发初始化互相踩踏。
     */
    private val initMutex = Mutex()

    /**
     * 在途加载任务，用于 single-flight 合并相同请求，
     * 避免 N 个并发调用触发 N 次完整网络加载（惊群）。
     */
    private var inFlight: Deferred<List<MsgDetailEntity>>? = null

    /**
     * 所有的系统消息
     */
    private val systemMsgList = CopyOnWriteArrayList<MsgDetailEntity>()

    /**
     * 固件升级的列表信息
     */
    private var upgradeList = CopyOnWriteArrayList<MsgDetailEntity>()

    /**
     * 结果回调监听
     */
    private var mOnResult: ((List<MsgDetailEntity>?) -> Unit)? = null

    /**
     * 小红点的监听
     */
    private var hasNewMsgLiveData = MutableLiveData<Boolean>()

    override fun initMsgList(onResult: (List<MsgDetailEntity>?) -> Unit) {
        mOnResult = onResult
        initAllMsg()
    }

    /**
     * 获取设备升级的列表
     *
     * @return List<MsgDetailEntity>
     */
    override fun getDevUpgradeList(): List<MsgDetailEntity> {
        return upgradeList
    }

    /**
     * 获取所有未读消息
     *
     * @return 未读消息数量
     */
    override fun getUnreadMsgCount(unReadMsgCount: (Int) -> Unit) {
        initMsgList(onResult = {
            var unReadCount = 0
            for (msg in systemMsgList) {
                msg.let {
                    GwellLogUtils.i(TAG, "msg.unread ${msg.unreadCnt}, total $unReadCount")
                    unReadCount += it.unreadCnt
                }
            }
            unReadMsgCount.invoke(unReadCount)
        })
    }

    override suspend fun cleanUnreadMsgCount() {
        systemMsgList.forEachIndexed { index, msgDetailEntity ->
            if (msgDetailEntity.unreadCnt > 0) {
                msgDetailEntity.unreadCnt = 0
            }
            GwellLogUtils.i(TAG, "map $msgDetailEntity")
            when (msgDetailEntity.tag) {
                MsgDetailEntity.TAG_MSG_CENTER_APP_UPGRADE -> {
                    val curVersion = Version.from(msgDataStore.getAppUpgradeRead() ?: "")
                    val tagVersion = Version.from(msgDetailEntity.appVersion ?: "")
                    if (tagVersion > curVersion) {
                        msgDataStore.setAppUpgradeRead(msgDetailEntity.appVersion ?: "")
                    }
                }

                MsgDetailEntity.TAG_MSG_CENTER_FIRMWARE_UPDATE -> {
                    msgDataStore.setDevUpgradeRead(
                        msgDetailEntity.deviceId.toString(),
                        msgDetailEntity.appVersion ?: ""
                    )
                }

                else -> {
                    msgCenterRepository.readMsg(null, null)
                        .onSuccess {
                            GwellLogUtils.i(TAG, "onSuccess: readSystemMsg success")
                        }
                        .onServerError { code, msg ->
                            GwellLogUtils.e(TAG, "onServerError: code $code, msg $msg")
                        }
                        .onLocalError {
                            GwellLogUtils.e(TAG, "onLocalError: it $it")
                        }
                }
            }
        }
    }

    override suspend fun getUnreadMsgCount(): Int {
        val msgList = loadAllMsg()
        var unReadCount = 0
        for (msg in msgList) {
            GwellLogUtils.i(TAG, "msg.unread ${msg.unreadCnt}, total $unReadCount")
            unReadCount += msg.unreadCnt
        }
        return unReadCount
    }

    override suspend fun readMessage(
        tag: String?,
        deviceId: String?
    ): Boolean {
        if (tag.isNullOrEmpty() && deviceId.isNullOrEmpty()) {
            cleanUnreadMsgCount()
            return true
        }
        try {
            val deviceId = deviceId?.toLong()
            val result = msgCenterRepository.readMsg(tag = tag, deviceId = deviceId)
            when (result) {
                is Success -> {
                    GwellLogUtils.i(TAG, "readMessage success ${result.data}")
                    return true
                }

                is LocalError -> {
                    GwellLogUtils.e(TAG, "readMessage error: ${result.t.message}")
                }

                is ServerError -> {
                    GwellLogUtils.e(
                        TAG,
                        "readMessage error: code ${result.code}, msg ${result.msg}"
                    )
                }
            }
        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "readMessage error: ${e.message}")
            false
        }
        return false
    }

    /**
     * 初始化所有消息
     */
    private fun initAllMsg(onResult: ((List<MsgDetailEntity>?) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            val msgList = loadAllMsg()
            onResult?.invoke(msgList)
            if (onResult == null) {
                mOnResult?.invoke(msgList)
            }
        }
    }

    /**
     * 加载所有系统消息。
     *
     * 优化一(single-flight)：并发调用共享同一次加载，避免多次网络请求；
     * 由 [initMutex] 做创建竞态的双检锁，任务结束后释放 [inFlight]。
     */
    private suspend fun loadAllMsg(): List<MsgDetailEntity> {
        inFlight?.let { return it.await() }
        return initMutex.withLock {
            inFlight?.let { return it.await() }
            scope.async(Dispatchers.IO) {
                try {
                    doLoad()
                } finally {
                    inFlight = null
                }
            }
        }.also { inFlight = it }.await()
    }

    /**
     * 实际拉取逻辑。
     *
     * 优化二(失败兜底)：先写入临时快照，全部成功后一次性覆盖成员列表；
     * 任一子请求失败时，保留已拉取到的部分或上次快照，避免未读数"假清零"。
     */
    private suspend fun doLoad(): List<MsgDetailEntity> {
        val snapshot = ArrayList<MsgDetailEntity>()
        val upgradeSnapshot = ArrayList<MsgDetailEntity>()
        try {
            if (gwiotOpt.showAppUpgradeItem()) {
                initAppVersionMsg(snapshot)
            }
            initDevUpgradeMsg(upgradeSnapshot, snapshot)
            initServerMsg(snapshot)
            // 全部成功后一次性覆盖
            systemMsgList.clear()
            systemMsgList.addAll(snapshot)
            upgradeList.clear()
            upgradeList.addAll(upgradeSnapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            GwellLogUtils.e(TAG, "doLoad failed, keep previous snapshot", e)
            // 部分成功则保留已拉取到的数据，否则维持上次快照，避免假清零
            if (snapshot.isNotEmpty()) {
                systemMsgList.clear()
                systemMsgList.addAll(snapshot)
            }
            if (upgradeSnapshot.isNotEmpty()) {
                upgradeList.clear()
                upgradeList.addAll(upgradeSnapshot)
            }
        }
        return ArrayList(systemMsgList)
    }

    /**
     * 初始化app版本升级消息
     */
    private suspend fun initAppVersionMsg(target: MutableList<MsgDetailEntity>) {
        msgCenterRepository.getAppUpdateMsg()
            .onSuccess {
                this?.let {
                    val appUpdateMsg = initAppUpgrade(it)
                    val currentVersionName = VersionUtils.getAppVersionName(app)
                    GwellLogUtils.i(
                        TAG,
                        "appUpdateMsg: $appUpdateMsg, currentVersionName $currentVersionName"
                    )
                    if (Version.from(appUpdateMsg.appVersion ?: "") > Version.from(
                            currentVersionName
                        )
                    ) {
                        target.add(appUpdateMsg)
                    }
                }
            }
            .onServerError { code, msg ->
                GwellLogUtils.e(TAG, "getAppUpdateMsg error: code $code, msg $msg")
            }
            .onLocalError {
                GwellLogUtils.e(TAG, "getAppUpdateMsg onLocalError: it ${it.message}")
            }
    }

    /**
     * 初始化固件升级的消息
     */
    private suspend fun initDevUpgradeMsg(
        upgradeSnapshot: MutableList<MsgDetailEntity>,
        systemSnapshot: MutableList<MsgDetailEntity>
    ) {
        // 初始化固件升级消息
        msgCenterRepository.getUpdateDeviceMsg()?.let {
            GwellLogUtils.i(TAG, "getUpdateDeviceMsg $it")
            upgradeSnapshot.clear()
            val list = initDevUpgrade(it)
            GwellLogUtils.i(TAG, "initDevUpgrade $list")
            upgradeSnapshot.addAll(list)
            if (list.isNotEmpty()) {
                systemSnapshot.add(list.first())
            }
        } ?: GwellLogUtils.i(TAG, "getUpdateDeviceMsg is null")
    }

    /**
     * 初始化服务器消息
     */
    private suspend fun initServerMsg(target: MutableList<MsgDetailEntity>) {
        // 获取消息列表数据
        msgCenterRepository.getMsgList()
            .onSuccess {
                GwellLogUtils.i(TAG, "MsgListEntity: $this")
                this?.list?.run {
                    initCustomerMsg(this, target)
                }
                GwellLogUtils.i(TAG, "systemMsgList: $systemMsgList")
            }
            .onServerError { code, msg ->
                GwellLogUtils.e(TAG, "onServerError: code $code, msg $msg")
            }
            .onLocalError {
                GwellLogUtils.e(TAG, "onLocalError: error ${it.message}")
            }
    }

    /**
     * app 升级消息
     *
     * @param entity AppUpgradeEntity? app升级信息
     * @return MsgDetailEntity? 消息
     */
    private fun initAppUpgrade(entity: AppUpgradeEntity): MsgDetailEntity {
        return entity.run {
            val curVersion = Version.from(msgDataStore.getAppUpgradeRead() ?: "")
            val tagVersion = Version.from(versionNo)
            val unReadCount = if (tagVersion > curVersion) {
                1
            } else {
                0
            }
            MsgDetailEntity(
                MsgDetailEntity.TAG_MSG_CENTER_APP_UPGRADE,
                0, System.currentTimeMillis() / 1000, false,
                app.getString(R.string.AA0495),
                GwStringUtils.formatStr(
                    app.getString(R.string.AA0496),
                    versionNo
                ),
                unReadCount,
                "",
                0,
                "",
                versionNo
            )
        }
    }

    /**
     * 将固件升级信息 转为 消息
     *
     * @param entityList List<VersionInfoEntity>
     * @return List<MsgDetailEntity>
     */
    private fun initDevUpgrade(entityList: List<VersionInfoEntity>): List<MsgDetailEntity> {
        return entityList.mapNotNull {
            val deviceId = it.deviceId
            if (deviceId.isNullOrEmpty() || it.version.isEmpty()) {
                null
            } else {
                val haveReadUpdate = msgDataStore.getDevUpgradeRead(deviceId)
                val currentUpdate = buildString {
                    append(deviceId)
                    append(it.version)
                }
                val unReadCount = if (haveReadUpdate == currentUpdate) 0 else 1
                MsgDetailEntity(
                    MsgDetailEntity.TAG_MSG_CENTER_FIRMWARE_UPDATE,
                    deviceId.toLong(),
                    System.currentTimeMillis() / 1000,
                    true,
                    app.getString(R.string.AA0219),
                    GwStringUtils.formatStr(
                        app.getString(R.string.AA0497),
                        it.devName,
                        it.version
                    ),
                    unReadCount,
                    "",
                    0,
                    "",
                    it.version
                )
            }
        }
    }

    /**
     * 处理客服消息
     *
     * @param msgList List<Msg?>
     * @param target  写入的系统消息快照列表
     */
    private fun initCustomerMsg(msgList: List<MsgDetailEntity>, target: MutableList<MsgDetailEntity>) {
        val customerMsg = ArrayList<MsgDetailEntity>()
        for (msg in msgList) {
            // 区分客服消息和其他消息（客服消息需要App处理是否有详情页）
            GwellLogUtils.i(TAG, "msg $msg")
            if (MsgDetailEntity.TAG_MSG_CENTER_CUSTOMER_SRV == msg.tag) {
                customerMsg.add(msg)
            } else {
                target.add(msg)
            }
        }
        // 如果有两条以上的客服消息
        if (customerMsg.size > 1) {
            val msg = customerMsg[0]
            msg.isHeap = true
            msg.customerMsg = ArrayList()
            msg.customerMsg?.addAll(initCustomerMsgInfo(customerMsg))
            target.add(msg)
        } else {
            target.addAll(customerMsg)
        }
        GwellLogUtils.i(TAG, "customerMsg: $customerMsg")
    }

    /**
     * 初始化客服详情
     * @param customerMsg   客服消息
     * @return 客服详情消息
     */
    private fun initCustomerMsgInfo(customerMsg: ArrayList<MsgDetailEntity>): ArrayList<MsgInfoListEntity.MSGInfo> {
        val customerMsgInfo = ArrayList<MsgInfoListEntity.MSGInfo>()
        for (msg in customerMsg) {
            msg.run {
                val msgInfo = MsgInfoListEntity.MSGInfo(
                    0L,
                    tag,
                    deviceId,
                    12,
                    title,
                    summary,
                    msgTime,
                    redirectUrl
                )
                customerMsgInfo.add(msgInfo)
            }
        }
        return customerMsgInfo
    }

}