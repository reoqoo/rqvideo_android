package com.gw.cp_config.impl

import android.app.Application
import com.gw.cp_config.BuildConfig
import com.gw_reoqoo.cp_account.api.kapi.IAccountApi
import com.gw.cp_config.api.IAppConfigApi
import com.gw.cp_config.api.ProductImgType
import com.gw.cp_config.data.datastore.IConfigDataStoreApi
import com.gw.cp_config.data.repository.ConfigRepository
import com.gw.cp_config.entity.DevConfigEntity
import com.gwell.loglibs.GwellLogUtils
import com.tencentcs.iotvideo.http.interceptor.flow.HttpAction
import com.therouter.router.getStringFromAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/6 20:18
 * Description: GlobalApiImpl
 */
@Singleton
class AppConfigApiImpl @Inject constructor(
    private val accountApi: IAccountApi,
) : IAppConfigApi {

    companion object {
        private const val TAG = "AppConfigApiImpl"
    }

    private val scope = MainScope()

    /**
     * 是否使用本地配置（用于调试使用）
     */
    private val testConfig = false

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var repository: ConfigRepository

    @Inject
    lateinit var api: IConfigDataStoreApi

    private val mutex = Mutex()

    /**
     * 异步更新配置
     */
    override fun uploadConfig() {
        scope.launch(Dispatchers.IO) {
            updateConfigSync()
        }
    }

    /**
     * 协程同步的方式进行更新配置，带锁
     */
    override suspend fun updateConfigSync(retry: Int) {
        mutex.withLock { updateConfigSyncInner(retry) }
    }

    /**
     * 协程同步的方式进行更新配置,无锁
     */
    private suspend fun updateConfigSyncInner(retry: Int) {
        GwellLogUtils.i(TAG, "updateConfigSyncInner(retry=$retry)")
        if (retry < 0) {
            GwellLogUtils.e(TAG, "updateConfigSyncInner(retry=$retry)")
            return
        }
        // 测试数据
        if (testConfig) {
            GwellLogUtils.i(TAG, "updateConfigSyncInner.testConfig = true")
            initConfigFromAssets()
            return
        }
        val isLogin = accountApi.isAsyncLogin()
        val action = repository.getAppConfigAction(isLogin)
        GwellLogUtils.i(TAG, "updateConfigSyncInner(retry=$retry),action=$action,isLogin=$isLogin")
        when {
            action is HttpAction.Success && action.data != null -> {
                val entity = action.data ?: throw Exception("action.data == null")
                // 支持短信推送的国家码列表，如不支持则不能通过手机号码注册
                val countryCodeList = entity.countryCodeList
                // 信息地址（json 格式，app自定义维护）
                val productConfUrl = entity.productConfUrl
                // 产品配置信息版本（app根据版本判断是否加载最新配置）
                val productConfVer = entity.productConfVer

                if (countryCodeList != null) {
                    GwellLogUtils.i(TAG, "countryCodeList $countryCodeList")
                    api.setCountryCodeList(countryCodeList)
                }
                // 本地配置文件
                val configFile = File(repository.getConfigFilePath())
                // 如果本地配置文件存在，且版本号也相等，则不用更新
                if (api.getProductConfVer() == productConfVer && configFile.isFile && configFile.exists()) {
                    GwellLogUtils.i(TAG, "app configVersion is equals remote ConfigVersion")
                    repository.initConfigFile()
                    return
                }
                // 这里需要下载更新配置文件
                if (productConfUrl.isNullOrEmpty()) {
                    // 下载地址没有的话，是会出大问题的
                    GwellLogUtils.e(TAG, "productConfUrl == null")
                    return updateConfigSyncInner(retry - 1)
                }
                val downloadSuccess =
                    repository.downloadFile(productConfUrl, configFile.absolutePath)
                if (!downloadSuccess) {
                    // 如果下载没有成功，则有问题啊
                    GwellLogUtils.e(
                        TAG,
                        "downloadFile field:url=$productConfUrl,path=${configFile}"
                    )
                    return updateConfigSyncInner(retry - 1)
                }
                // 下载成功
                // 更新本地版本号
                api.setProductConfVer(productConfVer)
                // 更新时间
                api.setConfigUpdateTime(System.currentTimeMillis())
                // 从文件中加载
                repository.initConfigFile()

                GwellLogUtils.i(TAG, "updateConfigSyncInner success")
            }

            else -> {
                // 获取失败了
                if (retry > 0) {
                    // 重试
                    GwellLogUtils.e(TAG, "updateConfigSyncInner field retry and -1")
                    updateConfigSyncInner(retry - 1)
                } else {
                    // 没拿到配置信息就初始化本地获取配置文件
                    GwellLogUtils.e(TAG, "updateConfigSyncInner field , will initConfigFromAssets")
                    initConfigFromAssets()
                }
            }
        }
    }

    override fun getCountryCodeList(): List<String> {
        val districtCodeList = api.getCountryCodeList()
        return if (districtCodeList.isNullOrEmpty()) {
            listOf("86")
        } else {
            districtCodeList
        }
    }

    override fun getSystemScenes(language: String, country: String?): List<String>? {
        GwellLogUtils.i(TAG, "language $language")
        return when (language) {
            "zh" -> {
                if (country == "HK" ||
                    country == "TW" ||
                    country == "MO"
                ) {
                    api.getSceneName()?.hant
                } else {
                    api.getSceneName()?.hans
                }
            }

            "vi" -> api.getSceneName()?.vi
            "th" -> api.getSceneName()?.th
            "ko" -> api.getSceneName()?.ko
            "ja" -> api.getSceneName()?.ja
            "in" -> api.getSceneName()?.android_in
            "ms" -> api.getSceneName()?.ms
            else -> api.getSceneName()?.en
        }
    }

    /**
     * 获取所有设备配置信息
     *
     * @return Map<String, DevConfigEntity>?
     */
    override fun getDevConfig(): Map<String, DevConfigEntity>? = api.getProductPid()

    /**
     * 通过设备pid来获取设备配置信息
     *
     * @param pid String        产品Pid
     * @return DevConfigEntity? 设备配置信息
     */
    override fun getDevConfigByPid(pid: String): DevConfigEntity? {
        val entity = repository.getProductPid(pid)
        GwellLogUtils.i(TAG, "entity: $entity")
        return entity
    }

    /**
     * 根据产品ID获取产品图片
     *
     * @param pid String?             产品Pid
     * @param imgType ProductImgType  产品图片类型ProductImgType
     * @return String? ⾸⻚、设备列表⻚、分享设备⻚共⽤图
     * @return String? 配置Wi-Fi⻚、设备控制⻚、共享管理⻚共⽤图
     */
    override fun getProductImgUrl(pid: String?, imgType: ProductImgType): String? {
        return getDevConfig()?.get(pid)?.productImageURL_A
    }

    /**
     * 根据产品ID获取产品图片
     * @return String? 配置Wi-Fi⻚、设备控制⻚、共享管理⻚共⽤图
     */
    override fun getProductImgUrl_C(pid: String?, imgType: ProductImgType): String? {
        return getDevConfig()?.get(pid)?.productImageURL_C
    }

    /**
     * 根据产品ID获取产品图片
     * @return String?  设备连接⻚、设备分享弹窗
     */
    override fun getProductImgUrl_D(pid: String?, imgType: ProductImgType): String? {
        return getDevConfig()?.get(pid)?.productImageURL_D
    }

    /**
     * 根据产品id获取产品名称（自动国际化）
     */
    override fun getProductName(pid: String): String? {
        val entity = getDevConfigByPid(pid)
        val appLocal = app.resources.configuration.locale
        // zh
        val language = appLocal.language
        // hk tw hans
        val country = appLocal.country
        return entity?.productName
    }

    override fun getPermissionMode(): Int {
        return api.getPermissionMode()
    }

    override fun setPermissionMode(mode: Int) {
        api.setPermissionMode(mode)
    }

    /**
     * 用APP内置的文件进行初始化
     */
    private suspend fun initConfigFromAssets() {
        GwellLogUtils.i(TAG, "initConfigFromAssets")
        val configJson = getStringFromAssets(
            app.applicationContext,
            BuildConfig.APP_CONFIG_FILE_NAME
        )
        repository.initConfigFile(configJson)
    }

}