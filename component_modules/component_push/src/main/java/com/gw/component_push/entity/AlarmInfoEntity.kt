package com.gw.component_push.entity

import com.squareup.moshi.Json

/**
 * 功能解释:
 *
 * @date 2020/11/10
 */
data class AlarmInfoEntity(
    /**
     * data : {"alarmId":"42949674971605009023","alarmType":1,"deviceId":4294967497,"endTime":1605009031,"firstAlarmType":1,"imgUrl":"https://iotvideo-pictstore-1259781373.cos.ap-guangzhou.myqcloud.com/2020-11-10%2F4294967497%2F?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDjkX6RXw5WifVFloAKI1ED0Yqvqa5kL6b%26q-sign-time%3D1605009023%3B1605012623%26q-key-time%3D1605009023%3B1605012623%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3D75b9b31ca690185b4dea18653d30ad8aa0a802b2","startTime":1605009023,"thumbUrlSuffix":"&imageMogr2/thumbnail/213x120"}
     */
    @Json(name = "data")
    var data: AlarmInfo? = null,
)

data class AlarmInfo(
    /**
     * alarmId : 42949674971605009023
     * alarmType : 1
     * deviceId : 4294967497
     * endTime : 1605009031
     * firstAlarmType : 1
     * imgUrl : https://iotvideo-pictstore-1259781373.cos.ap-guangzhou.myqcloud.com/2020-11-10%2F4294967497%2F?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDjkX6RXw5WifVFloAKI1ED0Yqvqa5kL6b%26q-sign-time%3D1605009023%3B1605012623%26q-key-time%3D1605009023%3B1605012623%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3D75b9b31ca690185b4dea18653d30ad8aa0a802b2
     * startTime : 1605009023
     * thumbUrlSuffix : &imageMogr2/thumbnail/213x120
     */
    @Json(name = "alarmId")
    var alarmId: String,

    @Json(name = "alarmType")
    var alarmType: Int = 0,

    @Json(name = "deviceId")
    var deviceId: Long = 0,

    @Json(name = "endTime")
    var endTime: Int = 0,

    @Json(name = "firstAlarmType")
    var firstAlarmType: Int = 0,

    @Json(name = "imgUrl")
    var imgUrl: String? = null,

    @Json(name = "startTime")
    var startTime: Int = 0,

    @Json(name = "thumbUrlSuffix")
    var thumbUrlSuffix: String? = null,
)