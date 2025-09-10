package com.gw_reoqoo.component_family.api.impl

import com.gw_reoqoo.component_family.api.interfaces.IAppFrontApi
import android.content.Context
import com.gw_reoqoo.component_family.utils.SimpleProcessLifecycleListener
import com.jwkj.base_lifecycle.process_lifecycle.ProcessLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFrontApiImpl @Inject constructor(
    @ApplicationContext val context: Context
) : IAppFrontApi, SimpleProcessLifecycleListener {
    /**
     * APP是否挂在后台？
     */
    private var isAppInBackground: Boolean? = null

    init {
        ProcessLifecycleManager.registerProcessLifecycleListener(this)
    }

    override fun isAppInBackground(): Boolean? {
        return isAppInBackground
    }

    override fun onBackground() {
        this.isAppInBackground = true
    }

    override fun onForeground() {
        this.isAppInBackground = false
    }
}