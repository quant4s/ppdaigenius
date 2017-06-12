package com.p2pgenius.user

/**
  * Created by joe on 2017/5/19.
  */
/**
  *
  * @param code 0: 表示正常
  * @param message  消息
  */
case class Result(code: Int = 0, message: String = "", content: Any = None)
