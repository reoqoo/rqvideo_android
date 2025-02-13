package com.gw.reoqoo.app

import android.app.Application
import android.util.Log
import com.gw.lib_router.ReoqooRouterInitializer
import com.gw.lib_utils.StorageInitTask
import com.gw.lib_utils.file.StorageUtils
import com.gw.log.Constants.APP_LOG_CACHE_PREFIX
import com.gw.log.ReoqooLogInitTask
import com.gw.module_mount.initializetask.InitializeTaskDispatcher
import com.gw.reoqoo.BuildConfig
import com.gw.resource.R
import com.gwell.loglibs.GwellLogUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 *@Description: 主进程模块初始化
 *@Author: ZhangHui
 *@Date: 2023/7/19
 */
@Singleton
class MainProcessApp @Inject constructor() : IProcessApp {
    companion object {
        private const val TAG = "MainProcessApp"
    }

    @Inject
    lateinit var appCoreInitTask: AppCoreInitTask

    @Inject
    lateinit var autoSizeInitTask: AutoSizeInitTask

    override lateinit var appContext: Application

    override fun mount(application: Application) {
        this.appContext = application
        Log.i(TAG, "MainProcessApp mount start")
        val cachePath = appContext.filesDir.path + File.separator + APP_LOG_CACHE_PREFIX
        // 根据app的名字动态修改log日志的文件名
        val logPrefix = application.getString(R.string.AA0447).replace(" ", "") + "_"
        // 执行初始化任务
        InitializeTaskDispatcher.createDispatcher()
            .addInitializeTask(StorageInitTask(appContext))
            .addInitializeTask(
                ReoqooLogInitTask(StorageUtils.getIotLogDir(appContext), cachePath, 10, logPrefix)
            )
            .addInitializeTask(appCoreInitTask)
            .addInitializeTask(ReoqooRouterInitializer(BuildConfig.DEBUG))
            .addInitializeTask(autoSizeInitTask)
            .start()
        GwellLogUtils.i(TAG, "MainProcessApp mount over")
    }

}