package com.gw.cp_mine.app

import android.util.Log
import com.gw.lib_router.BuildConfig
import com.gw.lib_router.ReoqooRouterInitializer
import com.gw.lib_utils.StorageInitTask
import com.gw.lib_utils.file.StorageUtils
import com.gw.log.Constants
import com.gw.log.ReoqooLogInitTask
import com.gw.module_mount.app.BaseApplication
import com.gw.module_mount.initializetask.InitializeTaskDispatcher
import dagger.hilt.android.HiltAndroidApp
import java.io.File

/**
 *@Description: 壳子Application
 *@Author: ZhangHui
 *@Date: 2023/7/18
 */
//@HiltAndroidApp
class ReoqooApplication : BaseApplication() {

    companion object {
        private const val TAG = "ReoqooApplication"
    }

    override fun onCreate() {
        super.onCreate()
        val logPath = StorageUtils.getIotLogDir(this)
        val cachePath = this.filesDir.path + File.separator + Constants.APP_LOG_CACHE_PREFIX
        Log.d(TAG, "onCreate")
        InitializeTaskDispatcher.createDispatcher()
            .addInitializeTask(StorageInitTask(this))
            .addInitializeTask(ReoqooLogInitTask(logPath, cachePath))
            .addInitializeTask(ReoqooRouterInitializer(BuildConfig.DEBUG))
            .start()
    }

}