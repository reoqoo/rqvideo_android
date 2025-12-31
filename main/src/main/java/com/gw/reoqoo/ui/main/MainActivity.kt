package com.gw.reoqoo.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.view.Gravity
import android.view.MenuInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.android.material.tabs.TabLayout
import com.gw.component_debug.api.interfaces.IAppEvnApi
import com.gw.component_plugin_service.api.IPluginManager
import com.gw.component_push.api.interfaces.IPushApi
import com.gw_reoqoo.house_watch.receivers.api.INetworkStatusApi
import com.gw_reoqoo.lib_base_architecture.ToolBarLoadStrategy
import com.gw_reoqoo.lib_base_architecture.view.ABaseMVVMDBActivity
import com.gw_reoqoo.lib_router.ReoqooRouterPath
import com.gw_reoqoo.lib_router.RouterParam.PARAM_NEED_LOGIN
import com.gw_reoqoo.lib_router.createFragment
import com.reoqoo.main.R
import com.reoqoo.main.databinding.AppActivityMainBinding
import com.gw.reoqoo.ui.fragment.FamilyFragment
import com.gw.reoqoo.ui.fragment.HouseKeepingFragment
import com.gw.reoqoo.ui.fragment.MineFragment
import com.gw_reoqoo.house_watch.ui.video_page.VideoPageFragment
import com.gwell.loglibs.GwellLogUtils
import com.jwkj.base_utils.ui.DensityUtil
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@Route(path = ReoqooRouterPath.AppPath.MAIN_ACTIVITY_PATH, params = [PARAM_NEED_LOGIN, "true"])
class MainActivity : ABaseMVVMDBActivity<AppActivityMainBinding, MainVM>() {
    companion object {
        private const val TAG = "MainActivity"
        private const val FRAGMENT_KEY = "FRAGMENT_KEY_%d"
        private const val CURRENT_ITEM_KEY = "CURRENT_ITEM_ID"
    }

    override fun getLayoutId() = R.layout.app_activity_main
    override fun <T : ViewModel?> loadViewModel() = MainVM::class.java as Class<T>

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var pushApi: IPushApi

    @Inject
    lateinit var networkStatusApi: INetworkStatusApi

    @Inject
    lateinit var appEnvApi: IAppEvnApi

    @Inject
    lateinit var pluginManager: IPluginManager

    private val fragmentsMap = LinkedHashMap<Int, Fragment?>()
    private var currentItemId: Int = R.id.navigation_family

    override fun initView() {
        mToolBarConfig.toolBarLoadStrategy = ToolBarLoadStrategy.NO_TOOLBAR
        // 初始化TabLayout
        initTabLayout()
        // 调试模式右上角增加角标
        if (appEnvApi.isDebugEnvMode()) {
            val img = ImageView(this)
            img.setImageResource(com.gw_reoqoo.resource.R.drawable.gw_reoqoo_debug_icon)
            val params =
                FrameLayout.LayoutParams(DensityUtil.dip2px(this, 40), DensityUtil.dip2px(this, 40))
            params.gravity = Gravity.END
            addContentView(img, params)
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        super.initData(savedInstanceState)
        // 启动监听网络状态的广播
        networkStatusApi.startReceiver()
        // 刷新用户信息
        mViewModel.refreshUserInfo()
        if (savedInstanceState != null) {
            val familyFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FRAGMENT_KEY.format(R.id.navigation_family)
            )
            fragmentsMap[R.id.navigation_family] = familyFragment
            val houseWatchFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FRAGMENT_KEY.format(R.id.navigation_house_keeping)
            )
            fragmentsMap[R.id.navigation_house_keeping] = houseWatchFragment
            val mineFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                FRAGMENT_KEY.format(R.id.navigation_mine)
            )
            fragmentsMap[R.id.navigation_mine] = mineFragment
            currentItemId = savedInstanceState.getInt(CURRENT_ITEM_KEY, R.id.navigation_family)
        }

        setFragment(currentItemId)
    }

    override fun onViewLoadFinish() {
        setStatusBarColor(mLight = true)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        //super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        for ((id, fragment) in fragmentsMap) {
            if (fragment == null) continue
            supportFragmentManager.putFragment(outState, FRAGMENT_KEY.format(id), fragment)
        }
        outState.putInt(CURRENT_ITEM_KEY, currentItemId)
        super.onSaveInstanceState(outState)
    }

    /**
     * 跳转到指定的fragment
     */
    private fun setFragment(itemId: Int) {
        this.currentItemId = itemId
        var fragment = fragmentsMap[itemId]
        if (fragment == null) {
            fragment = when (itemId) {
                R.id.navigation_family -> {
                    val familyFragment: Fragment = ReoqooRouterPath
                        .Family
                        .FAMILY_FRAGMENT_PATH
                        .createFragment<Fragment>()
                        ?: FamilyFragment()
                    familyFragment
                }

                R.id.navigation_house_keeping -> {
                    val houseWatchFragment: Fragment = ReoqooRouterPath
                        .HouseWatch
                        .FRAGMENT_MAIN
                        .createFragment<Fragment>()
                        ?: HouseKeepingFragment()
                    houseWatchFragment
                }

                R.id.navigation_mine -> {
                    val mineFragment: Fragment = ReoqooRouterPath
                        .MinePath
                        .FRAGMENT_MAIN
                        .createFragment<Fragment>()
                        ?: MineFragment()
                    mineFragment
                }

                else -> return
            }
            fragmentsMap[itemId] = fragment
        }
        VideoPageFragment.MAIN_CURRENT_INDEX = when (itemId) {
            R.id.navigation_family -> 0
            R.id.navigation_house_keeping -> 1
            R.id.navigation_mine -> 2
            else -> -1
        }
        GwellLogUtils.i(TAG, "setFragment: MAIN_CURRENT_INDEX == ${VideoPageFragment.MAIN_CURRENT_INDEX}")
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(mViewBinding.menuFrame.id, fragment)
        }
        fragmentsMap
            .values
            .filterNotNull()
            .filter { it != fragment }
            .forEach(transaction::hide)
        transaction.commitNowAllowingStateLoss()
    }

    /**
     *  添加TabLayout初始化方法
     */
    @SuppressLint("RestrictedApi")
    private fun initTabLayout() {

        // 从menu资源添加Tab项（与原BottomNavigationView使用相同的menu资源）
        val menuInflater = MenuInflater(this)
        val menu = MenuBuilder(this)
        menuInflater.inflate(R.menu.app_bottom_nav_menu, menu)

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            mViewBinding.tabLayoutMain.addTab(mViewBinding.tabLayoutMain.newTab()
                .setIcon(menuItem.icon)
                .setText(menuItem.title))
        }

        // 设置Tab选中监听（替换原BottomNavigationView的选中监听）
        mViewBinding.tabLayoutMain.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // 根据选中的Tab切换对应的Fragment
                val tabId = when (tab.position) {
                    0 -> R.id.navigation_family
                    1 -> R.id.navigation_house_keeping
                    2 -> R.id.navigation_mine
                    else -> R.id.navigation_family
                }
                setFragment(tabId)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}