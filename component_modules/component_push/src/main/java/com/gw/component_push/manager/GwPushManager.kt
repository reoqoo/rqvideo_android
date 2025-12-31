package com.gw.component_push.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.gw.component_push.BuildConfig
import com.gw.component_push.api.interfaces.INotifyServer
import com.gw.component_push.datastore.PushDataStore
import com.gw.cp_config.api.AppChannelName
import com.gw.component_push.entity.UploadTokenBean
import com.gw_reoqoo.cp_account.api.kapi.IAccountApi
import com.gw.cp_config.api.IAppParamApi
import com.gw_reoqoo.lib_utils.ktx.getAppVersionName
import com.gw_reoqoo.lib_http.wrapper.HttpServiceWrapper
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.iotvideo.init.IoTVideoInitializer
import com.jwkj.lib_gpush.manager.GPushMgr
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import com.yoosee.lib_gpush.GPushManager
import com.yoosee.lib_gpush.entity.PushChannel
import com.yoosee.lib_gpush.listener.IGPushNotificationCallback
import com.yoosee.lib_gpush.listener.IGPushResultListener
import com.yoosee.lib_gpush.strategy.SingleStrategy
import com.yoosee.lib_gpush.utils.DevicePushUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/7/20 14:02
 * Description: 推送消息管理类
 */
@Singleton
class GwPushManager @Inject constructor(
    private val httpService: HttpServiceWrapper,
    private val iAccountApi: IAccountApi,
    private val appParamApi: IAppParamApi,
    private val dataStore: PushDataStore,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GwPushManager"

        /**
         * 操作系统类型,2是iOS,3是安卓
         */
        private const val OS_TYPE = 3
    }

    private val scope by lazy { MainScope() }

    private val notifyServers = mutableListOf<INotifyServer>()

    /**
     * 推送SDK是否初始化成功
     */
    private var isPushInitSuccess = false

    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    /**
     * Push服务初始化
     *
     * @param context Context 上下文
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun init() {
        val appContext = context.applicationContext
        val userId = runBlocking { iAccountApi.getAsyncUserId() }
        GwellLogUtils.i(TAG, "init-userId:$userId")
        if (userId.isNullOrEmpty()) {
            return
        }

        // 优先注册通知的监听器
        registerNotifyListener()
        // 初始化推送SDK(全部渠道)
        val singleStrategy = SingleStrategy(40000, getCurrentChannel())

        GPushManager.init(appContext, singleStrategy, object : IGPushResultListener {
            override fun onFailure(errCode: String, errString: String) {
                GwellLogUtils.e(TAG, "init failure: errCode=$errCode, errString=$errString")
                isPushInitSuccess = false
//                register()
            }

            override fun onSuccess(channel: String, token: String) {
                GwellLogUtils.i(TAG, "init success: channel=$channel, token=$token")
                GlobalScope.launch(Dispatchers.IO) {
                    isPushInitSuccess = true
                    runBlocking { dataStore.saveToken(userId, token) }
                    register()
                }
            }
        })
    }

    /**
     * 向服务器注册推送服务
     *
     * @param context Context          上下文
     * @param dataStore PushDataStore  推送数据存储
     * @param iAccountApi IAccountApi  账号api
     * @param appParamApi IAppParamApi 应用参数api
     */
    fun register() {
        if (!isPushInitSuccess) {
            GwellLogUtils.i(TAG, "register: isPushInitSuccess false")
            init()
            return
        }
        val userId = runBlocking { iAccountApi.getAsyncUserId() }
        GwellLogUtils.i(TAG, "init-userId:$userId")
        if (userId.isNullOrEmpty()) {
            return
        }
        val token = runBlocking { dataStore.getToken(userId) }
        GwellLogUtils.i(TAG, "init-token:$token")

        if (token.isNullOrEmpty()) {
            GwellLogUtils.i(TAG, "init token is null")
            return
        }

        val userInfo = runBlocking { iAccountApi.getSyncUserInfo() }
        if (userInfo?.terminalId.isNullOrEmpty()) {
            GwellLogUtils.i(TAG, "init terminalId is null")
            return
        }

        handler.let {
            it.removeCallbacksAndMessages(null)
            it.postDelayed({
                scope.launch {
                    registerPush(userId, token, userInfo?.terminalId?: "")
                }
            }, 1000)
        }

    }

    /**
     * 向服务器注册推送服务
     *
     * @param userId String  用户id
     * @param token String   推送token
     * @param terminalId String 终端id
     */
    private suspend fun registerPush(userId: String, token: String, terminalId: String) {
        val channel = getCurrentChannel()
        // 系统类型
        val osType = OS_TYPE
        // 时区（单位秒）
        val timeZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000
        // 语言
        val language = Locale.getDefault().language ?: "zh"
        // appId
        val appId = appParamApi.getAppID()
        // 手机唯一Id
        val phoneId = getPhoneUniqueId()
        // 极光推送Id
        val jPushId = ""
        // 系统版本
        val osVer = Build.VERSION.RELEASE
        // sdk版本
        val sdkVer = "1.0"
        // app版本
        val appVer = context.getAppVersionName()
        // 系统推送Id
        val osPushId = ""
        // 制造商推送Id
        val mfrPushId = token
        // 制造商名称
        val mfrName = channel.channel
        // 手机型号
        val mfrDevModel = Build.MODEL
        GwellLogUtils.i(TAG, "registerPush -> $userId, $token, $channel")

        val uploadTokenBean = UploadTokenBean(
            appId = appId,
            appName = appParamApi.getAppName(),
            appToken = appParamApi.getAppToken(),
            appVersion = appVer ?: "",
            clearOtherTerm = false,
            jpushId = jPushId,
            language = language,
            mfrDevModel = mfrDevModel,
            mfrName = mfrName,
            mfrPushId = mfrPushId,
            osPushId = osPushId,
            osVer = osVer,
            phoneId = phoneId,
            pkgName = context.packageName,
            region = Locale.getDefault().language ?: "zh",
            sdkVer = sdkVer,
            termId = terminalId,
            terminalOS = osType,
            zone = timeZone.toLong()
        )

        val flow = httpService.uploadTokenFlow(
            uploadTokenBean.appId,
            uploadTokenBean.appName,
            uploadTokenBean.appToken,
            uploadTokenBean.language,
            uploadTokenBean.pkgName,
            uploadTokenBean.terminalOS,
            uploadTokenBean.appVersion,
            uploadTokenBean.region,
            uploadTokenBean.termId,
            uploadTokenBean.zone,
            uploadTokenBean.osVer,
            uploadTokenBean.sdkVer,
            uploadTokenBean.phoneId,
            uploadTokenBean.jpushId,
            uploadTokenBean.osPushId,
            uploadTokenBean.mfrPushId,
            uploadTokenBean.mfrName,
            uploadTokenBean.mfrDevModel,
            uploadTokenBean.clearOtherTerm,
        )

        flow.collect { action ->
            when (action) {
                is HttpAction.Loading -> {
                    GwellLogUtils.i(TAG, "registerPush onStart...")
                }

                is HttpAction.Fail -> {
                    GwellLogUtils.e(TAG, "registerPush onError IoTError", action.t)
                    dataStore.saveTokenPushStatus(userId, token, false)
                }

                is HttpAction.Success -> {
                    GwellLogUtils.i(TAG, "registerPush onSuccess -> ${action.data}")
                    dataStore.saveTokenPushStatus(userId, token, true)
                    GwellLogUtils.i(TAG, "registerPush onSuccess -> saveToken")
                }
            }
        }
    }


    // 硬件信息拼接获取设备唯一Id(无需权限)
// TODO 这个函数 可以 优化 一下, 有 两个方面:
//              1) 只有 第一次 才进行 字串的拼接, 然后 缓存起来. 后继 去 获取 时, 直接 返回 所缓存的值
//              2) 要 考虑 多进程情况, 不同的进程所返回的值 是不是 一样的. 当然, 从 函数的代码来看, 不同的进程 所返回的值 是 一样的.
    private fun getPhoneUniqueId(): String {
        val serial = "serial"
        // 硬件信息拼接
        val deviceIdSplit =
            Build.BOARD + Build.BRAND + Build.DEVICE + Build.MANUFACTURER + Build.PRODUCT + Build.SERIAL
        return UUID(deviceIdSplit.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }

    /**
     * 解析intent中的消息
     *
     * @param intent Intent 意图
     * @return String 推送消息
     */
    fun getIntentExtra(intent: Intent): String {
        val extra = GPushMgr.getInstance().getPushExtrasData(intent)
        GwellLogUtils.i(TAG, "getIntentExtra: GPushMgr=$GPushMgr, intent=$intent, extra=$extra")
        return extra
    }

    /**
     * 注册消息通知的处理监听
     */
    private fun registerNotifyListener() {
        GPushManager.registerNotifyListener(object : IGPushNotificationCallback {

            override fun notifyClick(channel: PushChannel, msg: String) {
                GwellLogUtils.i(TAG, "notifyClick: channel=$channel, msg=$msg")
                for (notifyServer in notifyServers) {
                    notifyServer.onNotifyMsgClick(channel.channel, msg)
                }
            }

            override fun notifyReceiver(
                channel: PushChannel,
                msg: Bundle,
                title: String,
                content: String
            ) {
                GwellLogUtils.i(TAG, "notifyReceiver: channel=$channel, msg=$msg, title=$title, content=$content")
            }
        })
    }

    /**
     * 添加通知的订阅服务
     *
     * @param server INotifyServer 通知服务
     */
    fun addNotifyServer(server: INotifyServer) {
        if (!notifyServers.contains(server)) {
            notifyServers.add(server)
        }
    }

    /**
     * 清空通知消息的服务
     */
    fun cleanNotifyServer() {
        notifyServers.clear()
    }

    /**
     * 获取当前设备的推送通道
     * @return PushChannel 推送通道
     */
    private fun getCurrentChannel(): PushChannel {
        return if (BuildConfig.IS_GOOGLE) {
            PushChannel.BRAND_FCM
        } else {
            DevicePushUtils.getDevicePushChannel() ?:PushChannel.BRAND_FCM
        }
    }
}