package com.gw.cp_mine.data_store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gw.cp_mine.BuildConfig
import com.gw.cp_mine.entity.Language
import com.gw_reoqoo.lib_datastore.DataStoreUtils
import com.gwell.loglibs.GwellLogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale.ENGLISH
import javax.inject.Inject
import javax.inject.Singleton

/**
@author: xuhaoyuan
@date: 2023/9/7
description:
Description: 数据存储工具类
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "language")

@Singleton
class LocaleDataStoreImpl @Inject constructor(
    @ApplicationContext val context: Context,
) : ILocaleDataStoreApi {

    companion object {
        private const val TAG = "LocaleDataStoreImpl"

        private const val KEY_LANGUAGE_TYPE = "key_language_type"

        private const val KEY_COUNTRY_TYPE = "key_country_type"
    }

    private val dataStore = context.dataStore

    /**
     * 获取当前语言
     *
     * @return Language 当前语言
     */
    override fun getCurrentLanguage(): Language {
        val defaultLanguage = try {
            Language.valueOf(BuildConfig.DEFAULT_LANGUAGE)
        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "getDefaultLanguage error", e)
            Language.SYSTEM
        }
        val langName = DataStoreUtils.getData(dataStore, KEY_LANGUAGE_TYPE, defaultLanguage.name)
        val language = try {
            Language.valueOf(langName)
        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "getLanguageType", e)
            Language.SYSTEM
        }
        GwellLogUtils.i(TAG, "getCurrentLanguage: language = ${language.locale?.language}")
        return language
    }

    /**
     * 设置当前语言
     *
     * @param language Language 语言
     */
    override fun setCurrentLanguage(language: Language) {
        DataStoreUtils.putData(dataStore, KEY_LANGUAGE_TYPE, language.name)
    }

    /**
     * 获取当前国家
     *
     * @return String 当前国家
     */
    override fun getCurrentCountry(): String {
        // 优先取APP的国家
        val appCountry = context?.resources?.configuration?.locale?.country
        val defaultLanguage = try {
            Language.valueOf(BuildConfig.DEFAULT_LANGUAGE)
        } catch (e: Exception) {
            GwellLogUtils.e(TAG, "getDefaultLanguage error", e)
            Language.SYSTEM
        }
        val defaultCountry = if (defaultLanguage.locale?.country.isNullOrEmpty()) {
            appCountry
        } else {
            defaultLanguage.locale?.country
        }
        GwellLogUtils.i(TAG, "getCurrentCountry: defaultLanguage = $defaultLanguage, defaultCountry = $defaultCountry, appCountry = $appCountry")
        return DataStoreUtils.getData(dataStore, KEY_COUNTRY_TYPE, defaultCountry?: ENGLISH.country)
    }

    /**
     * 设置当前国家
     *
     * @param country String 国家
     */
    override fun setCurrentCountry(country: String) {
        DataStoreUtils.putData(dataStore, KEY_COUNTRY_TYPE, country)
    }
}

interface ILocaleDataStoreApi {

    /**
     * 获取当前语言
     *
     * @return Language 当前语言
     */
    fun getCurrentLanguage(): Language

    /**
     * 设置当前语言
     *
     * @param language Language 语言
     */
    fun setCurrentLanguage(language: Language)

    /**
     * 获取当前国家
     *
     * @return String 当前国家
     */
    fun getCurrentCountry(): String

    /**
     * 设置当前国家
     *
     * @param country String 国家
     */
    fun setCurrentCountry(country: String)

}
