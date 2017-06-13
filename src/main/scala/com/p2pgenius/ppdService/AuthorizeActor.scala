package com.p2pgenius.ppdService

import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.p2pgenius.persistence.PpdUser
import com.ppdai.open.{RsaCryptoHelper, _}
import org.json4s.jackson.JsonMethods.parse

/**
  * 完成授权的过程
  * 1. 授权
  * 2. 获取用户名
  * 3. 获取余额
  */
class AuthorizeActor extends PpdRemoteService with Actor with ActorLogging{
  override val connection: HttpURLConnection = createUrlConnection(QUERY_BALANCE_URL)
  val queryUsernameConn = createUrlConnection(QUERY_USERNAME_URL)
  val authorizeConn = createUrlConnection(AUTHORIZE_URL)

  override def receive: Receive = {
    case ServiceAction(ServiceActionType.AUTHORIZE_USER, au) =>authorize(au.asInstanceOf[(String, String)]._1)
//    case au: AuthorizeUser => authorize(au.code)
  }


  def authorize(code: String): Unit = {
    log.debug("引导用户授权")
    val result = send(authorizeConn, "{\"AppID\":\"%s\",\"code\":\"%s\"}".format(APP_ID, code), accessToken = "")    // 授权
    val authInfo = if(result.sucess) {
          val jv = parse(result.context)
          jv.extract[AuthInfo]
        } else
          null.asInstanceOf[AuthInfo]

    log.debug("获取用户名")
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val d = new Date()
//    OpenApiClient.Init(APP_ID, com.ppdai.open.core.RsaCryptoHelper.PKCSType.PKCS8, SERVER_PUBLIC_KEY, CLIENT_PRIVATED_KEY)
//    val r = OpenApiClient.send(QUERY_USERNAME_URL, authInfo.AccessToken, new com.ppdai.open.core.PropertyObject("OpenID", authInfo.OpenID, com.ppdai.open.core.ValueTypeEnum.String))
    val json = "{\"OpenID\":\"%s\"}".format(authInfo.OpenID)
    val userInfoResult = send(queryUsernameConn, json, new PropertyObject("OpenID", authInfo.OpenID, ValueTypeEnum.String))(authInfo.AccessToken)// 用户名
    val userInfo = if(userInfoResult.sucess) {
      val jv = parse(userInfoResult.context)
      jv.extract[UserInfoResult]
    } else
      null.asInstanceOf[UserInfoResult]

    val ppdName =new RsaCryptoHelper(PKCSType.PKCS8, SERVER_PUBLIC_KEY, CLIENT_PRIVATED_KEY).decryptByPrivateKey(userInfo.UserName)
//    val ppdName = code

    val queryBalanceResult = send(connection, "")(authInfo.AccessToken)  // 查询余额
    val queryBalance = if(queryBalanceResult.sucess) {
      val jv = parse(queryBalanceResult.context)
      jv.extract[QueryBalanceResult]
    } else
      null.asInstanceOf[QueryBalanceResult]
    val balance =  queryBalance.Balance.find(b => b.AccountCategory == "用户备付金.用户现金余额").get.Balance
    log.debug("获取用户%s  余额:%s".format(ppdName,balance.toString))

    //

    log.info("通知用户管理Actor，创建一个新的用户")
    sender ! PpdUser(ppdName, balance, 0, 58, 0, 16, authInfo.OpenID, authInfo.AccessToken,
      authInfo.RefreshToken, authInfo.ExpiresIn.toInt, new Date())
  }
}

object AuthorizeActor {
  def props() = {
    Props(classOf[AuthorizeActor])
  }
}

