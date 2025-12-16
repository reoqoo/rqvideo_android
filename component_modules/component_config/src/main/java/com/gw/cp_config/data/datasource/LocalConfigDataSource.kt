package com.gw.cp_config.data.datasource

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gw.cp_config.BuildConfig
import com.gw.cp_config.data.datastore.IConfigDataStoreApi
import com.gw.cp_config.entity.ConfigJsonEntity
import com.gw.cp_config.entity.DevConfigEntity
import com.gw_reoqoo.lib_utils.file.StorageMgr
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_utils.file.GwFileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/6 20:20
 * Description: 本地获取配置信息的数据源
 */
class LocalConfigDataSource @Inject constructor(
    private val app: Application,
    private val api: IConfigDataStoreApi
) {

    companion object {
        private const val TAG = "LocalConfigDataSource"

        /**
         * 配置文件名
         */
        const val FILE_NAME_CONFIG_PID = BuildConfig.APP_CONFIG_FILE_NAME
    }

    val pidConfigPath: String
        get() {
            var appDocPath = StorageMgr.getInstance(app).appDocPath
            if (appDocPath.isNullOrEmpty()) {
                appDocPath = app.cacheDir.absolutePath
            }
            return "$appDocPath/$FILE_NAME_CONFIG_PID"
        }

    /**
     * 初始化产品配置文件，将json保存到DataStore中
     */
    suspend fun initConfig() {
        withContext(Dispatchers.IO) {
            val json = GwFileUtils.readFile2String(pidConfigPath)
            GwellLogUtils.i(TAG, "initConfig（） json===$json")
            initConfig(json)
        }
    }

    /**
     * 初始化产品配置文件，将json保存到DataStore中
     */
    suspend fun initConfig(configJson: String) {
        val configJsonEntity: ConfigJsonEntity? = Gson().fromJson(
            configJson,
            object : TypeToken<ConfigJsonEntity>() {}.type
        )
        GwellLogUtils.i(TAG, "configJsonEntity $configJsonEntity")
        if (configJsonEntity == null) {
            return
        }

        if (!configJsonEntity.products.isNullOrEmpty()) {
            api.setProductPid(configJsonEntity.products)
        } else {
            val products = configJsonEntity.productList.entries.map { (pid, entity) ->
                entity.pid = pid
                entity
            }
            api.setProductPid(products)
        }
        api.setSceneName(configJsonEntity.sceneList)
        if (configJsonEntity.platform != null) {
            api.setPermissionMode(configJsonEntity.platform.mode)
        }
    }

    fun getPermissionMode(): Int {
        return api.getPermissionMode()
    }

    /**
     * 通过Pid来获取产品的详细信息
     *
     * @param pid String 产品Pid
     * @return DevConfigEntity? 详细信息
     */
    fun getProductPid(pid: String): DevConfigEntity? {
        val products = api.getProductPid()
        if (products.isEmpty()) {
            try {
                val json = GwFileUtils.readFile2String(pidConfigPath)
                val configJsonEntity: ConfigJsonEntity? = Gson().fromJson(
                    json, ConfigJsonEntity::class.java
                )
                if (configJsonEntity == null) {
                    return null
                }
                if (!configJsonEntity.products.isNullOrEmpty()) {
                    return configJsonEntity.products.firstOrNull { it.pid == pid }
                }
                return configJsonEntity.productList[pid]
            } catch (e: Exception) {
                GwellLogUtils.e(TAG, "getProductPid error, pid=$pid")
                return null
            }
        }
        return products.firstOrNull { it.pid == pid }
    }

}