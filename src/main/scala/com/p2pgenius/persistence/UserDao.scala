package com.p2pgenius.persistence

import java.util.Date

import profile.simple._

/**
  * Created by joe on 2017/4/17.
  */
class UserDao(implicit session: Session) {

  def getUser(id: Int): Option[User] = {
    gUsers.filter(_.id === id).take(1).firstOption
    None
  }


  /**
    * 保存一个用户的授权
    * @param ppdUser
    */
  def createUser(ppdUser: PpdUser) = {
    val strategy1 = gPpdUsers.filter(_.ppdName === ppdUser.ppdName).take(1).firstOption
    if(!strategy1.isEmpty) {
      // 增加一个用户
      // val id = gUsers.map(u => (u.id, u.balance, u.lastBidTime, u.status)) += User(None, 0, new Date(), 0)
      // val id = ( gTraders returning gTraders.map(_.id) += entity)
      // ppdUser.uid = 1 // TODO
      gPpdUsers.map(s => s) += ppdUser
    }
    // 用户状态改为可用
  }

  /**
    * 为当前用户增加拍拍贷账户授权
    * @param ppdName
    */
  def attachUser(ppdName: String) = {

  }
}
