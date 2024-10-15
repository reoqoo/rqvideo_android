package com.gw.component_device_share.ui.qrcode_activity.adapter

import androidx.core.view.isVisible
import com.gw.component_device_share.R
import com.gw.lib_http.entities.Guest
import com.gw.lib_utils.InsensitiveUtils
import com.gw.resource.R as RR
import com.gw.lib_utils.ktx.loadUrl
import com.gw.component_device_share.databinding.DevShareItemNearShareListBinding as Binding
import com.gw.lib_utils.ktx.setSingleClickListener
import com.gw.lib_utils.ktx.visible
import com.gw.lib_widget.adapter.AbsDiffBDAdapter

/**
 * @Description: - 最近分享用户的Adapter
 * @Author: XIAOLEI
 * @Date: 2023/8/10
 */
class RecentlySharedAdapter : AbsDiffBDAdapter<Binding, Guest>() {
    override val layoutId: Int get() = R.layout.dev_share_item_near_share_list
    
    private var onItemClick: ((Guest) -> Unit)? = null
    
    private var guestId = ""
    
    override fun onCreateViewHolder(binding: Binding) {
        binding.root.setSingleClickListener { v ->
            val tag = v.tag
            if (tag is Guest) {
                onItemClick?.invoke(tag)
            }
        }
    }
    
    override fun onBindViewHolder(binding: Binding, position: Int) {
        val bean = getItemData(position)
        binding.root.tag = bean
        binding.tvUserName.text = bean.remarkName
        binding.splitLine.isVisible = position != itemCount - 1
        binding.tvAccount.text = InsensitiveUtils.getSecretAccount(bean.guestAccount ?: "")
        binding.ivChecked.visible(false)
//        if (guestId == bean.guestId) {
//            binding.ivChecked.setImageResource(R.drawable.dev_share_icon_user_check)
//        } else {
//            binding.ivChecked.setImageDrawable(null)
//        }
        
        binding.ivAvatar.loadUrl(
            bean.headUrl,
            placeHolder = RR.drawable.icon_default_avatar,
            error = RR.drawable.icon_default_avatar,
            circleCrop = true
        )
    }

    /**
     * 清除勾选
     */
    fun clearCheck() {
        this.updateChecked("")
    }
    
    /**
     * 当点击曾经分享的用户，获取到对应的用户账号，则更新勾选状态
     */
    fun updateChecked(account: String) {
        this.guestId = account
        notifyDataSetChanged()
    }
    
    /**
     * 获取已勾选的访客
     */
    fun getCheckedGuest(): Guest? {
        if (guestId.isEmpty()) return null
        return data.firstOrNull { it.guestId == guestId }
    }
    
    /**
     * Item点击事件
     */
    fun onItemClick(block: ((Guest) -> Unit)) {
        this.onItemClick = block
    }
}