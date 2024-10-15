# Reoqoo架构设计

# 一、项目背景

目前技威产品海外只有yoosee 方案线，公司决策新开一个项目做海外成品，考虑到Yoosee APP历史包袱太重以及UI的整体风格不符合海外的使用习惯，需要重新开发一个符合海外用户习惯的新APP，新应用点击设备列表中的设备拉起设备详情以及详情页衍生的下级页面复用“胖豚”插件代码，其他业务模块需要重新开发,项目于2023-08-01正式启动开发，排期开发时长1.5M。

# 二、功能模块介绍

## 2.1 功能模块图：

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/622c228e-23cc-4dd7-8585-7f3fbd2dd922.png)

## 2.2 UI原型：

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/02d6927a-c218-474c-be73-dd2a6ba65a94.png)

图1：主页UI原型图

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/6ebe1216-7eaa-4dfd-88a2-229bb581eed8.png)

 设备分享UI原型图

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/68657bab-562f-468e-a642-529f5dc89129.png)

配网UI效果图

# 三、app架构设计

## 3.1 架构设计图

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/yBRq17xJrvmQldv1/img/d7a3766f-3b4d-42ee-aa4a-cb01acb2c731.jpeg)

## 3.2 应用软件架构

应用采用MVVM架构模式，View层暂定使用xml+databind实现，前期不做强制要求使用Compose实现，View中的组件尽量使用Material库中的控件，ViewModel中共用逻辑代码抽离成单独的Case模块。

### 3.2.1各层职责划分：

*   UI Layer
    
    *   View层：
        
        *   负责UI搭建；
            
        *   显示数据状态；
            
        *   监听数据变化；
            
        *   传递用户交互动作给ViewModel；
            
    *   ViewModel层：
        
        *   从Repository获取数据并通知View层显示数据；
            
        *   管理“网域层”（ViewModel可复用代码，一般使用Case表示）代码使用
            
        *   处理业务逻辑
            
        *   处理View层发送过来的“Event”；
            
    *   网域层（Case，PS：不一定存在）：
        
        *   抽离ViewModel的可复用逻辑
            
*   Data Layer
    
    *   Repository层：
        
        *   负责暴露接口供ViewModel获取数据；
            
        *   管理数据的具体加载逻辑；
            
        *   管理DataResource的使用；
            
    *   DataResource
        
        *   负责数据的实际加载；
            

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/d2189064-9d0c-47bf-9489-6ea7596759cc.png)

# 四、开发规范

## 4.1 命名规范

1.  模块命名：
    
    *   业务组件：放在component\_modules文件下面，以component\_开头，e.g. component\_business
        
    *   基础组件：放在lib\_modules文件下面，以lib\_开头，e.g. lib\_utils
        
2.  包名：    
    

*   业务组件：com.gw.cp\_模块名称 e.g. com.gw.cp\_test  
    
    *   基础组件：com.gw.lib\_组件名称 e.g. com.gw.lib\_test 
        

1.  view：功能+类型，e.g. HomeActivity
    
2.  ViewModel：功能+VM，e.g. HomeVM
    
3.  Repository：数据+Repository，e.g. UserInfoRepository
    
4.  数据源:数据来源+DataResource，e.g. LocalUserInfoDataResource
    
5.  组件id：组件类型缩写\_功能名称，e.g. tv\_user\_name
    
6.  资源文件命名：防止资源冲突，强制使用文件前缀，每个组件build.gradle需要添加resourcePrefix，文件名称格式：模块名称\_文件名称，e.g. login\_icon\_arrow
    
7.  数据库命名：表和字段名称全小写，用\_拼接，禁止使用数据库关键字，如：name、time 、datetime、password 等
    

    android {
        /**
         * 资源文件前缀
         */
        resourcePrefix("login_")
    }

## 4.2 组件依赖

*   模块build.gradle，使用对应公共gradle，component组件apply component\_depends.gradle，lib组件apply lib\_depends.gradle
    
*   组件只能从上向下依赖，业务组件->基础组件，业务组件之间只能依赖组件api模块。
    

生成组件api模块方法：功能开发完后，修改需要对外提供的entity和interface文件后缀，java文件修改为.api，kotlin文件修改为.kapi，同步代码即可自动生成组件api模块，api模块代码不需要提交仓库

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/yBRq17xJrvmQldv1/img/babf229e-1203-4183-8358-911a90e8dc37.png)

## 4.3 resource

*   字符串
    

1.  做国际化适配，需要国际化的放在lib\_resource组件strings.xml
    
2.  待产品提供翻译，放在lib\_resource模块values/strings\_missing\_trans.xml
    
3.  不需要翻译，放在lib\_resource模块values/strings\_default.xml
    

*   图片、动画资源
    

1.  业务模块：自用图片、动画等资源，放在各自模块下，公共资源放在lib\_resource组件，例如共用图片、文本样式等
    
2.  组件模块：放在自己模块下
    

## 4.4 构建

*   APP打包需要支持aab和apk两种格式，Google Play Store要求新上架的应用打包格式需要是aab格式，国内应用市场目前只支持apk格式，但是目前产品只布局海外，所以前期只需要打包aab格式即可；
    
*   release版本需要从Jenkins上打包生成aab/apk，并且把生成的文件上传到腾讯cos云，方便测试和售后测试下载；
    
*   构建文件名称需要包含：应用名称+渠道（有则填写，没有不填写）+版本号+构建时间(年-月-日-时-分)+数据库版本+apk/aab；
    
*   签名证书秘钥不要明文存储在项目中，包含签名证书秘钥文件的文件不要提交到git仓库；
    

## 4.5 开发

*   新增的业务存代码需要使用Kotlin实现；
    
*   尽量多使用Material库控件；
    
*   多使用Jetpack组件；
    
*   鼓励使用Compose实现UI（考虑到项目开发周期，不强制使用）；
    
*   每个module需要有readme介绍该模块；
    
*   对于每个新的业务层module以及一些复杂的lib组件，开始正式功能编码前把module 的UML类图以及module结构图发出来评审一下（目的：了解大致的实现方案，评审是否有不合理的地方）；
    

# 五、基础组件技术选型

*   开发语言：Kotlin
    
*   应用基础架构组件base\_architecture
    

为MVVM结构提供基类，提供一些View或者ViewModel共用功能逻辑代码，大体的模块结构如下：

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/ybEnB2rkNr3GnP13/img/052fbb51-ae61-4e45-8101-41dbcbbc356e.png)

    class PlaceHolderActivity:ABaseMVVMActivity<PlaceHolderActivityVM>() {
        override fun onContentViewLoad(savedInstanceState: Bundle?) {
            setContentView(R.layout.activity_place_holder)
        }
    
        override fun <T : ViewModel?> loadViewModel():Class<T> {
            return PlaceHolderActivityVM::class.java as Class<T>
        }
    }

    package com.jwkj.lib_base_architecture.vm
    /**
     * @author: 2020
     *
     * @email: liukang@gwell.cc
     *
     * @date: 2022/3/1 19:12
     *
     * @description:占位Activity的ViewModel
     */
    class PlaceHolderActivityVM : ABaseVM() {
    
    }

*   键值对存储：DataStore
    

组件介绍：Android Jetpack 组件之一，它提供了一种基于协议缓存的数据存储方案，可以使用 Kotlin 协程进行异步访问 ，每个业务模块使用独立文件存储

使用示例：

    // 1. 创建DataSrore对象
    val dataStore = context.createDataStore(name = "user_settings")
    // 2. 获取保存key
    private val NAME_KEY = stringPreferencesKey("name")
    // 3. 存储数据
    dataStore.edit { settings ->
      settings[NAME_KEY] = userSettings.name
    }
    // 4. 获取数据
    dataStore.data.map { preferences ->
      val name = preferences[NAME_KEY] ?: "",
    }

*   网络：okhttp+Reotrofit+Flow，accountMgr库
    
*   数据库：room
    

组件介绍：Room 是 Android Jetpack 组件之一，它提供了一种在 SQLite 数据库上进行类型安全的抽象层，可以通过注解方式定义数据表和数据访问对象，使用起来比较方便。适用于需要存储结构化数据的场景

使用示例：

    // 1. 定义设备信息表
    @Entity(tableName = "device_info")
    data class DeviceInfo(
        @PrimaryKey @ColumnInfo(name = "id") val deviceId: String,
        @ColumnInfo(name = "device_name") val deviceName: String
    )
    
    // 2. 定义设备信息数据库访问对象
    @Dao
    interface DeviceInfoDao {
    
        /**
         * 通过id获取设备信息
    
         * @return
         */
        @Query("SELECT * FROM DeviceInfo WHERE id = :id")
        fun getDeviceInfoById(id: String): Flow<DeviceInfo>
    
        /**
         * 异步查询设备信息
         * @param id String
         * @return DeviceInfo
         */
        @Query("SELECT * FROM DeviceInfo WHERE id = :id")
        suspend fun getDeviceInfoByIdAsync(id: String): DeviceInfo
    
        /**
         * 获取所有设备信息
         */
        @Query("SELECT * FROM DeviceInfo")
        fun getAllDevice(): Flow<List<DeviceInfo>>
    
        /**
         * 添加或者更新设备信息，如果userId相同则会替换
    
         * @param deviceInfo
         */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertDevice(deviceInfo: DeviceInfo)
    
        /**
         * 删除所有设备信息
         */
        @Query("DELETE FROM DeviceInfo")
        fun deleteDevice()
    }
    
    // 3.1 添加数据库表
    @Database(entities = [UserInfo::class, DeviceInfo::class], version = 1)
    abstract class ReoqooDatabase : RoomDatabase() {
    
        /**
         * 获取UserInfo表访问对象
         * @return UserDao
         */
        abstract fun userDao(): UserDao
    
        /**
         * 3.2 获取DeviceInfo表
         * @return DeviceInfoDao
         */
        abstract fun deviceInfoDao(): DeviceInfoDao
    
        companion object {
    
            @Volatile
            private var INSTANCE: ReoqooDatabase? = null
    
            fun getInstance(context: Context): ReoqooDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }
    
            /**
             * 创建数据库
             * @param context Context
             * @return ReoqooDatabase
             */
            private fun buildDatabase(context: Context) =
                Room.databaseBuilder(
                    context.applicationContext,
                    ReoqooDatabase::class.java, "reoqoo.db"
                )
                    .build()
        }
    }

*   路由：TheRouter
    

组件介绍：TheRouter 是一个 Kotlin 编写，用于 Android 模块化开发的一整套解决方案框架。支持KSP、支持AGP8，不仅能对常规的模块依赖解耦、页面跳转，同时提供了模块化过程中常见问题的解决办法。[theRouter文档](https://therouter.cn/)

页面导航跳转能力（[Navigator](https://therouter.cn/docs/2022/08/28/01)） 页面跳转能力介绍

跨模块依赖注入能力（[ServiceProvider](https://therouter.cn/docs/2022/08/27/01)）跨模块依赖注入

单模块自动初始化能力 （[FlowTaskExecutor](https://therouter.cn/docs/2022/08/26/01)）单模块自动初始化能力介绍

动态化能力 ([ActionManager](https://therouter.cn/docs/2022/08/25/01)) 动态化能力支持

支持AGP8.0

AndroidStudio插件支持

    TheRouter.build(TestRouterPath.ROUTER_TEST_ACTIVITY)
                    .withString("key", "value")
                    .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .withSerializable("userInfo", userInfo)
                    .navigation()

    // 跳转路由
    @Route(path = TestRouterPath.ROUTER_TEST_ACTIVITY)
    class TestActivity : AppCompatActivity() {
    
        // 接收跳转参数
        @JvmField
        @Autowired
        var key: String? = null
    
        @JvmField
        @Autowired
        var userInfo: TestUserInfo? = null
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // 注入Activity，可写在基类
            TheRouter.inject(this)
        }
    
    }

            // 需在跳转前添加拦截器
            // 替换拦截，例如未登录跳转到登录界面
            addRouterReplaceInterceptor(object : RouterReplaceInterceptor() {
                override fun replace(routeItem: RouteItem?): RouteItem? {
                    // todo 调试代码，根据条件做页面替换，例如未登录跳转到登录界面
                    val isNeedLogin = routeItem?.params?.get("needLogin")?.toBoolean()
                    if (true == isNeedLogin) {
                        // 拦截跳转到登录界面
                        val target: RouteItem? = matchRouteMap(TestRouterPath.ROUTER_TEST_LOGIN_ACTIVITY)
                        target?.description = "可以在这修改路由参数，推荐把routeItem作为参数传给登录页"
                        return target
                    }
                    return routeItem
                }
            })
    
            // 全局拦截
            setRouterInterceptor(object : RouterInterceptor {
                override fun process(routeItem: RouteItem, callback: InterceptorCallback) {
                    // 可以在这拦截跳转，比如某些Debug页面在线上环境跳转
                    callback.onContinue(routeItem)
                }
            })
    
            // 默认全局跳转回调
            defaultNavigationCallback(object : NavigationCallback() {
                override fun onActivityCreated(navigator: Navigator, activity: Activity) {
                    super.onActivityCreated(navigator, activity)
                    GwellLogUtils.i(TAG, "onActivityCreated")
                }
    
                override fun onArrival(navigator: Navigator) {
                    super.onArrival(navigator)
                    GwellLogUtils.i(TAG, "onArrival")
                }
    
                override fun onFound(navigator: Navigator) {
                    super.onFound(navigator)
                    GwellLogUtils.i(TAG, "onFound")
                }
    
                override fun onLost(navigator: Navigator) {
                    super.onLost(navigator)
                    GwellLogUtils.i(TAG, "onLost")
                }
            })

*   模块通信：模块间通过api通信，由TheRouter依赖注入服务
    

    
    interface ITestService {
        fun getUserInfo(): TestUserInfo
    
        fun addUserInfo(userInfo: TestUserInfo)
    }
    
    @ServiceProvider
    class TestServiceImpl : ITestService {
        override fun getUserInfo(): TestUserInfo {
            return TestUserInfo("张三", "1")
        }
    
        override fun addUserInfo(userInfo: TestUserInfo) {
    
        }
    }

    TheRouter.get(ITestService::class.java)?.getUserInfo()

*   动态权限：permissionsdispatcher，通过注解方式调用，编译时注解自动生成权限请求代码，大    大简化 Android 应用程序中的权限请求代码编写，提高开发效率
    

    // 需要动态申请权限界面添加RuntimePermissions注解
    @RuntimePermissions
    class TestActivity : AppCompatActivity() {
    
        companion object {
            private const val TAG = "TestActivity"
        }
    
        private lateinit var viewBinding: TestActivityTestBinding
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            TheRouter.inject(this)
            viewBinding = TestActivityTestBinding.inflate(layoutInflater)
            setContentView(viewBinding.root)
            viewBinding.tvRequestPermission.setOnClickListener {
                // 调用需要权限的方法，会先检测权限，没有权限时先申请权限
                requestRecordAudioPermissionWithPermissionCheck()
            }
            GwellLogUtils.i(TAG, "key:$key userInfo:$userInfo")
        }
    
    
        /**
         * 申请权限
         */
        @NeedsPermission(Manifest.permission.RECORD_AUDIO)
        fun requestRecordAudioPermission() {
            // 权限申请成功后调用
            Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show()
        }
    
        /**
         * 不再申请权限后再次申请回调
         * @param request PermissionRequest
         */
        @OnShowRationale(Manifest.permission.RECORD_AUDIO)
        fun showRationale(request: PermissionRequest) {
            Toast.makeText(this, "申请录音权限", Toast.LENGTH_SHORT).show()
        }
    
        /**
         * 申请权限拒绝回调
         */
        @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
        fun onRecordAudioDenied() {
            Toast.makeText(this, "拒绝权限", Toast.LENGTH_SHORT).show()
        }
    
        /**
         * 不再申请权限回调
         */
        @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
        fun onRecordAudioNeverAskAgain() {
            Toast.makeText(this, "不再询问", Toast.LENGTH_SHORT).show()
        }
    
        
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            // NOTE: delegate the permission handling to generated function
            onRequestPermissionsResult(requestCode, grantResults)
        }
    
    }

*   插件与主工程通信方式
    

暂定接口形式与主工程通信

*   图片加载：Glide
    
*   与前端通信方案：原生JS交互，迁移Yoosee webview lib
    
*   UI实现方式：ViewBinding，Compose
    
*   暗黑模式适配：跟随系统暗黑模式，自定义适配，新建values-night和drawable-night资源目录存放暗色资源