package com.gw.reoqoo.app

import android.app.Activity
import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.TextView
import com.gw.reoqoo.R
import kotlin.system.exitProcess

/**
 * Root 设备拦截页
 * 全屏独占，无法通过返回键或 Home 键绕过
 */
class RootWarningActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root_warning)

        findViewById<TextView>(R.id.btnExit).setOnClickListener {
            killApp()
        }
    }

    /**
     * 禁用返回键，无法通过返回关闭
     */
    override fun onBackPressed() {
        // 空实现，什么都不做
    }

    /**
     * 用户通过 Home 键、最近任务、手势等离开此页面时，直接杀进程
     * 这是最关键的一行：让用户绝对绕不过此拦截
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        killApp()
    }

    private fun killApp() {
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}
