package com.gw_reoqoo.component_family.utils

import android.content.res.Configuration
import com.jwkj.base_lifecycle.process_lifecycle.listener.ProcessLifecycleListener

interface SimpleProcessLifecycleListener : ProcessLifecycleListener {
    override fun onForeground() {}
    override fun onBackground() {}
    override fun onTrimMemory(var1: Int) {}
    override fun onConfigurationChanged(config: Configuration) {}
    override fun onLowMemory() {}
}