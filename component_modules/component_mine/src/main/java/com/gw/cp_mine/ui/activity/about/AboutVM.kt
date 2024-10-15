package com.gw.cp_mine.ui.activity.about

import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.gw.lib_base_architecture.vm.ABaseVM
import com.gw.resource.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
@author: xuhaoyuan
@date: 2023/8/30
description:
1.
 */
@HiltViewModel
class AboutVM @Inject constructor() : ABaseVM() {

    val aboutMenuList = mutableListOf<AboutEnum>()

    init {
        aboutMenuList.clear()
        aboutMenuList.add(AboutEnum.VERSION_UPDATE)
        aboutMenuList.add(AboutEnum.USER_PROTOCOL)
        aboutMenuList.add(AboutEnum.PRIVACY_POLICY)
    }

}

enum class AboutEnum(val strRes: Int) {
    VERSION_UPDATE(R.string.AA0274),
    USER_PROTOCOL(R.string.AA0044),
    PRIVACY_POLICY(R.string.AA0368)
}