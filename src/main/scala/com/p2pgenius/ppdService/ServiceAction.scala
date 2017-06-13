package com.p2pgenius.ppdService

import com.p2pgenius.ppdService.ServiceActionType.ServiceActionType

/**
  * Created by joe on 2017/6/12.
  */
case class ServiceAction(action: ServiceActionType, body: Any = null)

object ServiceActionType extends Enumeration{
  type ServiceActionType = Value
  val AUTHORIZE_USER,     // 授权用户
    REMOVE_PPD_USER,    // 移除一个拍拍贷用户
    FETCH_MY_PPD_USER_LIST, // 获取我的授权拍拍贷用户列表
    LOAD_STRATEGY_INFO,   // 加载策略详细内容
    FETCH_MY_STRATEGY_LIST, // 获取我所有的策略，包括自定义和系统
    FETCH_MY_STRATEGY_lIST_SETTING,  // 获取我所有的策略设定
    INSERT_STRATEGY,  // 插入新策略
    UPDATE_STRATEGY,  // 更新策略
    UPDATE_STRATEGY_SETTING,  // 更新策略的设置
    UPDATE_GLOBAL_SETTING,  // 更新全局设置
    REMOVE_STRATEGY,  // 移除策略
    SUB_STRATEGY,   // 订阅
    UNSUB_STRATEGY, // 取消订阅
    SIGN_UP,  // 注册
    SIGN_IN,   // 登入
    CHECK_LOAN      // 检查借款标的，是否符合策略
  = Value
}
