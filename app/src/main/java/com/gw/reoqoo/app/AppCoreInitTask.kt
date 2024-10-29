package com.gw.reoqoo.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.MainThread
import com.google.gson.JsonObject
import com.gw.component_push.api.interfaces.IPushApi
import com.gw.cp_account.api.kapi.IAccountApi
import com.gw.cp_account.api.kapi.IAccountMgrApi
import com.gw.cp_account.api.kapi.IInterfaceSignApi
import com.gw.cp_account.api.kapi.IUserInfo
import com.gw.cp_config.api.IAppConfigApi
import com.gw.cp_config.api.IAppParamApi
import com.gw.cp_mine.api.kapi.ILocaleApi
import com.gw.lib_http.HttpResp
import com.gw.lib_http.error.ResponseCode
import com.gw.lib_http.jsonToEntity
import com.gw.lib_iotvideo.IoTSdkInitMgr
import com.gw.lib_utils.device_utils.PhoneIDUtils
import com.gw.lib_utils.file.StorageUtils
import com.gw.lib_utils.toast.IToast
import com.gw.module_mount.initializetask.AInitializeTask
import com.gw.module_mount.initializetask.TaskPriority
import com.gw.reoqoo.BuildConfig
import com.gw.reoqoo.app.crash.CrashCallbackImpl
import com.gw.reoqoo.ui.logo.LogoActivity
import com.gw.reoqoo.ui.main.MainActivity
import com.gw.reoqoosdk.dev_monitor.IMonitorService
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_crash.CrashManager
import com.jwkj.base_lifecycle.LifecycleInitializer
import com.jwkj.base_lifecycle.activity_lifecycle.ActivityLifecycleManager
import com.jwkj.base_lifecycle.activity_lifecycle.listener.ActivityLifecycleListener
import com.jwkj.base_statistics.GwStatisticsKits
import com.tencentcs.iotvideo.accountmgr.AccountMgr
import com.tencentcs.iotvideo.accountmgr.AccountMgr.PLATFORM_REOQOO
import com.tencentcs.iotvideo.http.interceptor.IInterceptorCallback
import com.tencentcs.iotvideo.http.interceptor.RedirectType
import com.therouter.router.Navigator
import com.therouter.router.RouteItem
import com.therouter.router.defaultNavigationCallback
import com.therouter.router.interceptor.InterceptorCallback
import com.therouter.router.interceptor.NavigationCallback
import com.therouter.router.interceptor.RouterInterceptor
import com.therouter.router.setRouterInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess
import com.gw.resource.R as RR

/**
 *@Description: 壳子初始化任务
 *@Author: ZhangHui
 *@Date: 2023/7/25
 */
@Singleton
class AppCoreInitTask @Inject constructor() : AInitializeTask() {

    companion object {
        private const val TAG = "AppCoreTask"

        /**
         * tencent bugly app id
         */
        private const val BUGLY_APP_ID = "6ca0576809"
    }

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var globalApi: IAppConfigApi

    @Inject
    lateinit var accountApi: IAccountApi

    @Inject
    lateinit var accountMgrApi: IAccountMgrApi

    @Inject
    lateinit var iMonitorService: IMonitorService

    @Inject
    lateinit var accountMgr: AccountMgr

    @Inject
    lateinit var toast: IToast

    @Inject
    lateinit var ioTSdkInitMgr: IoTSdkInitMgr

    @Inject
    lateinit var appParamApi: IAppParamApi

    @Inject
    lateinit var localeApi: ILocaleApi

    @Inject
    lateinit var pushApi: IPushApi

    @Inject
    lateinit var signApi: IInterfaceSignApi

    private val scope by lazy {
        MainScope()
    }

    override fun run() {
        GwellLogUtils.i(TAG, "AppCoreTask start")
        val app = context.applicationContext as Application
        initRouter(app)
        initLifecycle(app)
        initCrash(app)
        initIotSdk(app)
        initGwHttp(app)
        initStatistics(app)
        initPluginService(app)
        initAccountModule()
        initPushServer(app)
    }


    /**
     * 初始化账号模块
     */
    private fun initAccountModule() {
        accountApi.initAccount()
    }

    /**
     * 初始化插件服务
     *
     * @param application Application 上下文
     */
    private fun initPluginService(application: Application) {
        iMonitorService.register(application)
    }

    /**
     * 初始化路由
     */
    private fun initRouter(app: Application) {
        // 默认全局跳转回调
        defaultNavigationCallback(object : NavigationCallback() {
            override fun onActivityCreated(navigator: Navigator, activity: Activity) {
                super.onActivityCreated(navigator, activity)
                GwellLogUtils.i(TAG, "onActivityCreated")
            }

            override fun onArrival(navigator: Navigator) {
                super.onArrival(navigator)
                GwellLogUtils.i(TAG, "onArrival")
            }

            override fun onFound(navigator: Navigator) {
                super.onFound(navigator)
                GwellLogUtils.i(TAG, "onFound")
            }

            override fun onLost(navigator: Navigator, requestCode: Int) {
                super.onLost(navigator, requestCode)
                GwellLogUtils.i(TAG, "onLost")
            }
        })

        // 全局拦截
        setRouterInterceptor(object : RouterInterceptor {
            override fun process(routeItem: RouteItem, callback: InterceptorCallback) {
                // 可以在这拦截跳转，比如某些Debug页面在线上环境跳转
                callback.onContinue(routeItem)
            }
        })

    }

    override fun priority(): TaskPriority {
        return TaskPriority.PRIORITY_HIGH
    }

    /**
     * 生命周期模块的初始化
     */
    private fun initLifecycle(application: Application) {
        LifecycleInitializer.init(application, MainActivity::class.java)
        ActivityLifecycleManager.registerActivityLifecycleListener(object :
            ActivityLifecycleListener {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                // 对Application和Activity更新上下文的语言环境
                localeApi.initAppLanguage(p0)
            }

            override fun onActivityStarted(p0: Activity) {
            }

            override fun onActivityResumed(p0: Activity) {
            }

            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
                if (p0 is MainActivity) {
                    // 退出app
                    iMonitorService.closeFloatService()
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {
                GwellLogUtils.i(TAG, "initLifecycle : newConfig ${newConfig.locale.country}")
                localeApi.setCurrentCountry(newConfig.locale.country)
                localeApi.initAppLanguage()
            }

            override fun onLowMemory() {
            }

            override fun onTrimMemory(p0: Int) {
            }

        })
    }

    /**
     * 初始化网络请求库
     */
    private fun initGwHttp(app: Application) {
        // 注册对于类型的拦截器，注意，回调里是子线程
        accountMgr.registerTypeCache(JsonObject::class) { jsonObj ->
            val json = jsonObj.toString()
            GwellLogUtils.i(TAG, "json: $json")
            val baseResp = json.jsonToEntity<HttpResp<Any>>()
            if (baseResp?.isSuccess == false) {
                when (val respCode = ResponseCode.getRespCode(baseResp.code)) {
                    // 最多的错误提示
                    ResponseCode.CODE_10000 -> {
                        toast.show(respCode.msgRes)
                    }

                    ResponseCode.CODE_10902013 -> {
                        // 账号被注销，需要返回到启动页
                        GwellLogUtils.i(TAG, "respCode ${respCode.msgRes}")
                        accountMgrApi.loginFailure()
                        toast.show(respCode.msgRes)
                    }

                    ResponseCode.CODE_10026 -> {
                        // 查询设备在线状态接口失败时的返回值
                        GwellLogUtils.i(TAG, "respCode ${respCode.msgRes}")
                    }

                    ResponseCode.CODE_10012 -> {
                    }

                    !in ResponseCode.values() -> {
                        // 未知的错误提示
                        baseResp.msg?.let(toast::show)
                    }

                    else -> Unit
                }
            }
            GwellLogUtils.i(TAG, "jsonObj:$jsonObj")
        }
        // 注册统一的网络错误收集的回调
        val networkErrorFlow: Flow<Throwable> = callbackFlow {
            accountMgr.setOnNetworkError { t ->
                trySend(t)
            }
            awaitClose { cancel() }
        }

        scope.launch {
            // 取出JVM的运行时间，弹出吐司的时间频率为5秒一次，取出来的时间为纳秒，所以需要转换为毫秒
            var lastTime = System.nanoTime() / 1000000L
            networkErrorFlow.collect {
                val now = System.nanoTime() / 1000000L
                if (now - lastTime >= 5 * 1000) {
                    lastTime = now
                    toast.show(RR.string.AA0573)
                }
            }
        }

        val countryCode = localeApi.getCurrentCountry()
        accountMgr.init(
            "",
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            PLATFORM_REOQOO,
            BuildConfig.IS_GOOGLE,
            "",
            countryCode
        )
        // 设置地区
        accountMgr.setRegion(countryCode)
        accountMgr.setInterceptorCallback(object : IInterceptorCallback {
            override fun getAnonymousSignatureParams(): Array<String> {
                val anonymousInfo = signApi.getAnonymousInfo(app, appParamApi.getAppID())
                GwellLogUtils.i(TAG, "anonymousInfo: $anonymousInfo")
                return anonymousInfo
            }

            override fun onRedirectRequest(type: RedirectType): String {
                return ""
            }

        })
        accountMgr.setPlatformInfo(
            appParamApi.getAppName(),
            appParamApi.getAppID(),
            appParamApi.getAppToken()
        )

        // 更新到AccountMgr里作为基础信息
        fun updateAccountMgrBy(userInfo: IUserInfo?) {
            GwellLogUtils.i(TAG, "initGwHttp-$userInfo")
            val accessId = userInfo?.accessId
            val accessToken = userInfo?.accessToken
            val area = userInfo?.area
            val regRegion = userInfo?.regRegion ?: ""

            if (accessId != null && accessToken != null) {
                accountMgr.setAccessInfo(accessId, accessToken)
            }
            area?.let(accountMgr::setUserArea)
            regRegion.let(accountMgr::setRegRegion)
        }

        val userInfo = accountApi.getSyncUserInfo()
        updateAccountMgrBy(userInfo)
        accountApi.watchUserInfo().observeForever(::updateAccountMgrBy)
    }

    /**
     * 初始化统计库
     */
    @MainThread
    private fun initStatistics(appContext: Application) {
        GwStatisticsKits.init(appContext, BuildConfig.DEBUG, true)
    }

    /**
     * 初始化崩溃管理
     */
    private fun initCrash(app: Application) {
        if (StorageUtils.isDocPathAvailable(app)) {
            GwellLogUtils.i(TAG, "initCrash()")
            GwellLogUtils.i(TAG, "crash path: ${StorageUtils.getCrashLogDir(app)}")
            CrashManager.init(
                StorageUtils.getCrashLogDir(app),
                BUGLY_APP_ID,
                PhoneIDUtils.phoneUniqueId,
                CrashCallbackImpl(app)
            )
            // bugly的崩溃监听
            CrashManager.addBuglyCrashListener { uuid, crashType, errorType, errorMessage, errorStack ->
                GwellLogUtils.i(TAG, "bugly Crash")
                scope.launch(Dispatchers.IO) {
                    sleep(1000)
                    // 在此处执行重新启动应用程序的操作
                    val intent = Intent(app, LogoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    app.startActivity(intent)
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(2)
                }
                // saveCrashInfo(uuid, crashType, errorType, errorMessage, errorStack)
            }
        }
    }

    /**
     * 初始化iotSDK
     */
    private fun initIotSdk(app: Application) {
        GwellLogUtils.i(TAG, "initIotSdk()")
        ioTSdkInitMgr.initIoTSdk(app, false)
    }

    private fun initPushServer(app: Application) {
        GwellLogUtils.i(TAG, "initPush()")
        pushApi.initPushServer()
    }
}