package com.gw_reoqoo.cp_account.api.impl

import android.app.Application
import android.content.Context
import com.gw.cp_config.api.IAppParamApi
import com.gw_reoqoo.cp_account.api.kapi.IInterfaceSignApi
import com.gw_reoqoo.cp_account.kits.AccountMgrKit
import javax.inject.Inject


/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/11/30 15:37
 * Description: InterfaceSignApiImpl
 */
class InterfaceSignApiImpl @Inject constructor(
    val appParams: IAppParamApi
) : IInterfaceSignApi {

    override fun getAnonymousInfo(
        context: Context,
        appID: String,
        versionName: String
    ): Array<String> {
        val cid = appParams.getCid()
        return AccountMgrKit.getAnonymousSecureKey(context, cid, appID, versionName)
    }

}