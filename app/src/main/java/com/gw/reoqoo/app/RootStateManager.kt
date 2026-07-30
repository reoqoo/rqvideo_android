package com.gw.reoqoo.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.scottyab.rootbeer.RootBeer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.java

/**
 * Root 状态管理器（事件驱动版）
 * 通过监听 Activity 生命周期，在应用从后台回到前台时触发检测
 */
class RootStateManager private constructor(context: Context) {

    companion object {
        private const val TAG = "RootStateManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: RootStateManager? = null

        fun getInstance(context: Context): RootStateManager {
            return instance ?: synchronized(this) {
                instance ?: RootStateManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val rootBeer = RootBeer(context)

    // 当前 Root 状态
    private val _rootState = MutableStateFlow(false)
    val rootState: StateFlow<Boolean> = _rootState

    // 记录处于 Resumed 状态的 Activity 数量
    private var resumedCount = 0

    // 当前处于前台的 Activity
    private var currentActivity: Activity? = null

    // 是否已经触发过 Root 拦截
    @Volatile
    private var hasShownRootWarning = false

    /**
     * Activity 生命周期回调
     */
    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {

        override fun onActivityResumed(activity: Activity) {
            // currentActivity 必须在 checkRootStatus 之前赋值，
            // 否则首次弹框时获取不到有效的 Activity
            currentActivity = activity
            if (resumedCount == 0) {
                // 从后台切换到前台，触发 Root 检测
                checkRootStatus()
            }
            resumedCount++
        }

        override fun onActivityPaused(activity: Activity) {
            resumedCount--
            // 及时清理引用，避免持有已销毁的 Activity
            if (currentActivity === activity) {
                currentActivity = null
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity === activity) {
                currentActivity = null
            }
        }
    }

    /**
     * 注册生命周期监听
     */
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    /**
     * 注销生命周期监听
     */
    fun unregister(application: Application) {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    /**
     * 立即检测 Root 状态并更新 Flow
     */
    fun checkRootStatus(): Boolean {
        val isRooted = rootBeer.isRooted
        _rootState.tryEmit(isRooted)
        if (isRooted && !hasShownRootWarning) {
            showRootWarning()
        }
        return isRooted
    }

    /**
     * 启动 Root 拦截 Activity（独占全屏，无法绕过）
     */
    private fun showRootWarning() {
        val activity = currentActivity ?: run {
            Log.w(TAG, "showRootWarning skipped: currentActivity is null")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "showRootWarning skipped: activity is finishing or destroyed")
            return
        }

        hasShownRootWarning = true
        try {
            val intent = Intent(activity, RootWarningActivity::class.java).apply {
                // 清空当前任务栈所有 Activity，独占整个任务
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.startActivity(intent)
            Log.i(TAG, "showRootWarning: RootWarningActivity started")
        } catch (e: Exception) {
            // 启动失败时重置标记，下次有机会再次尝试
            hasShownRootWarning = false
            Log.e(TAG, "showRootWarning: failed to start RootWarningActivity", e)
        }
    }
}
