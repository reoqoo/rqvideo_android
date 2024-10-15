package com.gw.cp_msg.entity.http

import com.jwkj.lib_json_kit.IJsonEntity
import com.squareup.moshi.Json

/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/9/21 17:23
 * Description: VersionInfoEntity
 */
data class VersionInfoEntity(

    /**
     * 设备ID
     */
    @Transient
    var deviceId: String? = null,

    /**
     * 设备名称
     */
    @Transient
    var devName: String? = null,

    /**
     * 下载地址
     */
    @Json(name = "downUrl") private var _downUrl: String?,

    /**
     * 更新内容
     */
    @Json(name = "upgDescs") private var _upgDescs: String?,

    /**
     * 需要更新的版本
     */
    @Json(name = "version") private var _version: String?

) : IJsonEntity {
    val downUrl: String get() = _downUrl ?: ""
    val upgDescs: String get() = _upgDescs ?: ""
    val version: String get() = _version ?: ""
}