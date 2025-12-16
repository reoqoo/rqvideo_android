package com.gw.reoqoo.app

import android.app.Application
import android.util.Log
import com.gw.component_debug.api.interfaces.IAppEvnApi
import com.gw_reoqoo.lib_utils.StorageInitTask
import com.gw_reoqoo.lib_utils.file.StorageUtils
import com.gw_reoqoo.log.Constants.APP_LOG_CACHE_PREFIX
import com.gw_reoqoo.log.ReoqooLogInitTask
import com.gw_reoqoo.module_mount.initializetask.InitializeTaskDispatcher
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
    lateinit var addDebugApi: IAppEvnApi

    override lateinit var appContext: Application

    /**
     * 同步初始化
     */
    override fun initSync(application: Application) {
        this.appContext = application
        // 先初始化日志
        Log.i(TAG, "initSync")
        val cachePath = appContext.filesDir.path + File.separator + APP_LOG_CACHE_PREFIX
        ReoqooLogInitTask(
            StorageUtils.getIotLogDir(appContext),
            cachePath,
            addDebugApi.getLogsMaxNumber(),
            logLevelValue = addDebugApi.getLogLevelValue()
        ).initLog()
    }

    override suspend fun mount(application: Application) {
        this.appContext = application
        Log.i(TAG, "MainProcessApp mount start")
        // 执行初始化任务
        InitializeTaskDispatcher.createDispatcher()
            .addInitializeTask(StorageInitTask(appContext))
            .addInitializeTask(appCoreInitTask)
            .start()
        GwellLogUtils.i(TAG, "MainProcessApp mount over")
    }

}