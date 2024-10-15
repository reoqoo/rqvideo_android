package com.gw.cp_account.api.impl

import android.app.Application
import android.content.Context
import com.gw.cp_account.api.kapi.IInterfaceSignApi
import com.gw.cp_account.kits.AccountMgrKit
import javax.inject.Inject


/**
 * Author: yanzheng@gwell.cc
 * Time: 2023/11/30 15:37
 * Description: InterfaceSignApiImpl
 */
class InterfaceSignApiImpl @Inject constructor(
    private val app: Application
) : IInterfaceSignApi {
    override fun setAnonymousInfo(appID: String) {
    }

    override fun getAnonymousInfo(context: Context, appID: String): Array<String> {
        return AccountMgrKit.getAnonymousSecureKey(context, appID)
    }

}