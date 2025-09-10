package com.gw.reoqoo.app

import android.app.Application

/**
 *@Description: 模块进程初始化app
 *@Author: ZhangHui
 *@Date: 2023/7/19
 */
interface IProcessApp {

    var appContext: Application
    /**
     * 同步初始化
     */
    fun initSync(application: Application)
    
    /**
     * 挂载app
     * @param application Application
     */
    suspend fun mount(application: Application)
}