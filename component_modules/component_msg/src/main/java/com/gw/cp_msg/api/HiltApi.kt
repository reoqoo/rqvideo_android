package com.gw.cp_msg.api

import com.gw.cp_msg.api.interfaces.ILocalMsgApi
import com.gw.cp_msg.api.kapi.IMsgExternalApi
import com.gw.cp_msg.api.kapi.INoticeMgrApi
import com.gw.cp_msg.manger.LocalMsgExternalManager
import com.gw.cp_msg.manger.NoticeMgrImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Description: - 注册注入MsgModule的实现类
 * @Author: yanzheng
 * @Date: 2023/11/5
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class HiltApi {

    @Singleton
    @Binds
    abstract fun getMsgApi(impl: LocalMsgExternalManager): ILocalMsgApi

    @Singleton
    @Binds
    abstract fun getMsgExternalApi(impl: LocalMsgExternalManager): IMsgExternalApi

    @Singleton
    @Binds
    abstract fun getNoticeMsgApi(impl: NoticeMgrImpl): INoticeMgrApi

}
