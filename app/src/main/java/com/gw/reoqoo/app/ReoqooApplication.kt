package com.gw.reoqoo.app

import android.app.Activity
import android.content.res.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.gw.cp_mine.api.kapi.ILocaleApi
import com.gw.reoqoo.BuildConfig
import com.gw.reoqoo.ui.main.MainActivity
import com.gw_reoqoo.module_mount.app.BaseApplication
import com.gwell.loglibs.GwellLogUtils
import com.jakewharton.processphoenix.ProcessPhoenix
import com.jwkj.base_lifecycle.process_lifecycle.ProcessLifecycleManager
import com.jwkj.base_lifecycle.process_lifecycle.listener.ProcessLifecycleListener
import com.reoqoo.component_iotapi_plugin_opt.api.IGWIotOpt
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import com.reoqoo.main.R as MainR

/**
 *@Description: 壳子Application
 *@Author: ZhangHui
 *@Date: 2023/7/18
 */
@HiltAndroidApp
class ReoqooApplication : BaseApplication() {

    companion object {
        private const val TAG = "ReoqooApplication"
        private const val CHILD_THREAD_NAME = "LogSyncThread"
    }

    @Inject
    lateinit var gwIotOpt: IGWIotOpt

    @Inject
    lateinit var autoSizeInitTask: AutoSizeInitTask

    private val scope by lazy { MainScope() }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG,"onCreate")
        // 如果是重启进程，则不进行初始化操作
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            Log.i(TAG,"onCreate.isPhoenixProcess")
            return
        }
        ProcessLifecycleManager.init(this)
        Log.i(TAG, "onCreate: runBlocking.start")
        gwIotOpt.init(
            app = this@ReoqooApplication,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            appId = BuildConfig.APP_ID,
            appToken = BuildConfig.APP_TOKEN,
            appName = BuildConfig.APP_NAME,
            cId = BuildConfig.APP_CID,
            iotUrl = BuildConfig.WEBSITE_IOT_URL,
            baseUrl = BuildConfig.WEBSITE_HOST_BASE_URL,
            pluginUrl = BuildConfig.WEBSITE_PLUGIN_HOST_URL,
            h5Url = BuildConfig.WEBSITE_H5_HOST_URL,
            brandDomain = BuildConfig.QRCODE_DOMAIN,
            mainActvityKlass = MainActivity::class.java as Class<Activity>,
            aboutVersionUrl = BuildConfig.ABOUT_UPDATE_VERSION,
            baseTestUrl = BuildConfig.WEBSITE_HOST_BASE_TEST_URL,
            pluginTestUrl = BuildConfig.WEBSITE_PLUGIN_HOST_TEST_URL,
            h5TestUrl = BuildConfig.WEBSITE_H5_HOST_TEST_URL,
            yooseeShareOptionStr = BuildConfig.YOOSEE_SHARE_TYPES
        )
        Log.i(TAG, "onCreate: runBlocking. init finish")
        gwIotOpt.initUI(this@ReoqooApplication, MainR.string.appName)
        Log.i(TAG, "onCreate: runBlocking. initUI finish")
        gwIotOpt.autoAgreeProtocol()
        scope.launch {
            autoSizeInitTask.run()
            Log.i(TAG, "onCreate: runBlocking.finish")
        }
        WebView(this).destroy()
        // 获取当前系统语言
        GwellLogUtils.i(
            TAG,
            "system local ${Locale.getDefault().country}, language ${Locale.getDefault().language}"
        )
        // 设置主题
        setColorTheme()
        // 监听APP整体生命周期
        ProcessLifecycleManager.registerProcessLifecycleListener(object : ProcessLifecycleListener {
            private val handler: Handler

            init {
                val thread = HandlerThread(CHILD_THREAD_NAME)
                thread.start()
                handler = Handler(thread.looper)
            }

            override fun onForeground() = Unit
            override fun onBackground() {
                handler.post {
                    GwellLogUtils.flushLogFile(true)
                }
            }

            override fun onTrimMemory(p0: Int) = Unit
            override fun onConfigurationChanged(p0: Configuration) = Unit
            override fun onLowMemory() = Unit
        })
    }

    /**
     * 设置颜色主题
     */
    private fun setColorTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
