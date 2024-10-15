package com.gw.component_family.ui.device_list.adapter

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.view.View
import androidx.core.view.isVisible
import com.gw.component_family.R
import com.gw.resource.R as RS
import com.gw.component_family.databinding.FamilyItemDeviceListBinding
import com.gw.cp_config.api.IAppConfigApi
import com.gw.lib_room.device.DeviceInfo
import com.gw.lib_room.ktx.isMaster
import com.gw.lib_room.ktx.isOffline
import com.gw.lib_room.ktx.isOnline
import com.gw.lib_utils.ktx.draw
import com.gw.lib_utils.ktx.loadUrl
import com.gw.lib_utils.ktx.setSingleClickListener
import com.gw.lib_utils.ktx.visible
import com.gw.lib_widget.adapter.AbsDiffBDAdapter

/**
 * @Description: - 设备列表适配器
 * @Author: XIAOLEI
 * @Date: 2023/8/1
 */
class DeviceListAdapter(
    private var configApi: IAppConfigApi
) : AbsDiffBDAdapter<FamilyItemDeviceListBinding, DeviceInfo>() {

    /**
     * 点击item的事件
     */
    private var onItemClick: ((DeviceInfo) -> Unit)? = null

    /**
     * item长按事件
     */
    private var onItemLongClick: ((v: View, info: DeviceInfo) -> Unit)? = null

    /**
     * 点击关机/开机事件
     */
    private var onTurnOnOrOffClick: ((DeviceInfo, View, onClickConfirm: () -> Unit) -> Unit)? = null

    /**
     * 根据ID查询云服务是否开启
     */
    private var checkCloudOn: ((deviceId: String) -> Boolean?)? = null

    /**
     * 设置Item长按
     */
    fun setOnItemLongClick(onItemLongClick: ((v: View, info: DeviceInfo) -> Unit)) {
        this.onItemLongClick = onItemLongClick
    }

    /**
     * 设置Item点击事件
     */
    fun setOnItemClick(onItemClick: ((DeviceInfo) -> Unit)) {
        this.onItemClick = onItemClick
    }

    /**
     * 设置点击开机/关机按钮事件
     */
    fun setOnTurnOnOrOffClick(onTurnOnOrOffClick: ((DeviceInfo, View, onClickConfirm: () -> Unit) -> Unit)) {
        this.onTurnOnOrOffClick = onTurnOnOrOffClick
    }

    /**
     * 设置根据设备ID，查询云服务是否开启的函数
     */
    fun setCheckCloudOn(block: (deviceId: String) -> Boolean?) {
        this.checkCloudOn = block
    }

    override val layoutId: Int get() = R.layout.family_item_device_list


    override fun onCreateViewHolder(binding: FamilyItemDeviceListBinding) {
        // 点击设备
        binding.root.setSingleClickListener { v ->
            val tag = v.tag
            if (tag is DeviceInfo) {
                this.onItemClick?.invoke(tag)
            }
        }
        // 设备长按事件
        binding.root.setOnLongClickListener { v ->
            val tag = v.tag
            if (tag is DeviceInfo) {
                this.onItemLongClick?.invoke(v, tag)
            }
            true
        }
        // 点击关机/开机
        binding.btTurnOffOrOn.setSingleClickListener { view ->
            val tag = binding.root.tag
            if (tag is DeviceInfo) {
                if (tag.online == 0) {
                    // 设备离线状态，不可点击
                    return@setSingleClickListener
                }
                val onClickConfirm: () -> Unit = {
                    view.playAnimation()
                }
                this.onTurnOnOrOffClick?.invoke(tag, view, onClickConfirm)
            }
        }
        binding.btTurnOffOrOn.addAnimatorListener(object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                binding.btTurnOffOrOn.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator) {
                binding.btTurnOffOrOn.isEnabled = true
            }

            override fun onAnimationCancel(animation: Animator) {
                binding.btTurnOffOrOn.isEnabled = true
            }

            override fun onAnimationRepeat(animation: Animator) = Unit
        })
    }


    override fun onBindViewHolder(binding: FamilyItemDeviceListBinding, position: Int) {
        val info = this.getItemData(position)
        binding.root.tag = info
        binding.tvDevName.text = info.remarkName
        binding.btTurnOffOrOn.cancelAnimation()
        when {
            info.isOnline -> when (info.powerOn) {
                true -> {
                    binding.tvOnline.setText(RS.string.AA0053)
                    binding.tvOnline.draw(start = R.drawable.family_shape_online_point_online)
                    binding.btTurnOffOrOn.setAnimation(R.raw.family_dev_turnon)
                }

                false -> {
                    binding.tvOnline.setText(RS.string.AA0502)
                    binding.tvOnline.draw(start = R.drawable.family_shape_online_point_power_off)
                    binding.btTurnOffOrOn.setAnimation(R.raw.family_dev_turnoff)
                }

                else -> {

                }
            }

            info.isOffline -> {
                binding.tvOnline.setText(RS.string.AA0054)
                binding.tvOnline.draw(start = R.drawable.family_shape_online_point_offline)
                binding.btTurnOffOrOn.setImageResource(R.drawable.family_icon_btn_status_offline)
            }
        }

        binding.tvShare.isVisible = !info.isMaster
        binding.btTurnOffOrOn.isVisible = info.isMaster
        binding.btTurnOffOrOn.progress = 0f
        if (checkCloudOn?.invoke(info.deviceId) == null) {
            binding.ivCloud.visible(false)
            binding.vSplit.visible(false)
        } else {
            binding.ivCloud.visible(true)
            binding.vSplit.visible(true)
            val isCloudOn = checkCloudOn?.invoke(info.deviceId) ?: false
            binding.ivCloud.setImageResource(
                if (isCloudOn) {
                    R.drawable.family_icon_cloud_on
                } else {
                    R.drawable.family_icon_cloud_off
                }
            )
        }
        val productImg = configApi.getProductImgUrl(info.productId)
        binding.ivProduct.loadUrl(productImg)
    }


    override fun areIdSame(old: DeviceInfo, new: DeviceInfo) = old.deviceId == new.deviceId

    override fun areAllSame(old: DeviceInfo, new: DeviceInfo) = new == old
}