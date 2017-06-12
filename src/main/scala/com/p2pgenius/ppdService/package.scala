package com.p2pgenius

/**
  * Created by joe on 2017/3/30.
  */
package object ppdService {
  val LOGIN_CALLBACK_URL = "http://218.94.106.184:18080/ppd/login/"
  val PPD_LOGIN_URL="https://ac.ppdai.com/oauth2/login?"
  /**
    * APP ID
    */
  val APP_ID = "094e0ffd6cca46429e7c51751f1f0a3e"

  /**
    * 客户端私钥
    */
  val CLIENT_PRIVATED_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALsHzhbh4cyv4mKYVvGfvjSIv8TMSzFffozLslsioldmCk00xL1/unPCmUTu+z62OXEDHlWSVIDAMV56vPB4Wo9v/aSJ6DtDc7IqyGlum/QbzMV7FHvOzzhSvZlcQJHT1cxpLGT6I9wOqzs04wzzKGPJs5okRmZz6zCRE+i4INX5AgMBAAECgYA2W0tbPSTp30hVTV8fdETMcd4CLTnBTLaz5tOcRxGVgxOFYsu3I3MmB62R6j7c+ArzvGtJhXDHxpPSmKtm3CO9QeCVCIcf62nKWTZJOl3zD3ksUuH72Q0Nen4r89wFX+2WuXY2HX/OzYpybRPVVP/YO2ljWHpVv4PZm3IwMrNQ8QJBAPwYk/Z28P7mh03hEHRGfok/6Vo4ABj/sd6k6Sv4pO+9kWEKnDa6P1xcbnC2bbKgaUxmoy2g4/NuGlLhsJGGzQUCQQC97Ucu1rgVFW4GVQCO0kZdLNSnnGch5mTKk7BqqqRoS5dXCqivVWTEI9FbG19bxcZFGQwsnTo4pDXZ02NuNJdlAkEAo/Jj/6x4rH1Fz0soRUY3MpFC9C5pOdjIV3BOSA9rTJKvQjoBP8I+RJXEKT8q9jlGYa56PslclcqsCezGxM2y9QJAZp2Z1Su7/+A5NwCcMY5Y6CS9rOWrLzy8/lq3eZpDA/q7hRLEOx9HN1Ym2jO63OnJizHSSxSUDWBjnBJDaZrOpQJBAL0Sr0Z8hgHcszuByiUlTmHqXJKxSjS9OyXLq0Aaqcr4+/JK6FBaETV0jN+KlMzvf28iY2KMfMpU6uBGyGV3jOM="

  /**
    * 服务器端公钥
    */
  val SERVER_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC1vNUSeZwHR726oe3dy0O9kZ01zqVTCgzqdMQ0MCD5AP/les9hpN5itXC8m4XkhdAkJHUhqf7+d3AEjCxqEKvdVUn8FVppKbiTeGaAsJFHJuRcWwdP5abd2fxDtYZQm+6HL2jyFq8L0l6G+7gmBb+mq6k2BU1YqXRReZ+CEeUvkQIDAQAB"
//  val SERVER_PUBLIC_KEY = "QC1vNUSeZwHR726oe3dy0O9kZ01zqVTCgzqdMQ0MCD5AP/les9hpN5itXC8m4XkhdAkJHUhqf7+d3AEjCxqEKvdVUn8FVppKbiTeGaAsJFHJuRcWwdP5abd2fxDtYZQm+6HL2jyFq8L0l6G+7gmBb+mq6k2BU1YqXRReZ+CEeUvkQIDAQAB"
  /**
    *
    * 授权操作
    */
  val AUTHORIZE_URL = "http://ac.ppdai.com/oauth2/authorize"

  /**
    * 刷新AccessToken
    */
  val REFRESH_TOKEN_URL = "http://ac.ppdai.com/oauth2/refreshtoken"

  /**
    * 查询用户名
    */
  val QUERY_USERNAME_URL = "http://gw.open.ppdai.com/open/openApiPublicQueryService/QueryUserNameByOpenID"


  /**
    * 新版投标列表接口（默认每页2000条）, 返回投标列表
    */
  val LOAN_LIST_URL = "http://gw.open.ppdai.com/invest/LLoanInfoService/LoanList"

  /**
    * 新版散标详情批量接口（请求列表不大于10）
    */
  val LOAN_INFO_URL = "http://gw.open.ppdai.com/invest/LLoanInfoService/BatchListingInfos"

  /**
    * 投标接口,返回投标是否成功的信息
    */
  val BID_URL = "http://gw.open.ppdai.com/invest/BidService/Bidding"

  /**
    * 我的投标接口, 返回我的投标列表
    */
  val MY_BID_LIST_URL = "http://gw.open.ppdai.com/invest/BidService/BidList"

  /**
    * 获取用户资金余额
    */
  val QUERY_BALANCE_URL = "http://gw.open.ppdai.com/balance/balanceService/QueryBalance"

  /**
    * 新版列表状态查询批量接口（请求列表大小不大于20条）
    */
  val LOAN_STATUS_URL = "http://gw.open.ppdai.com/invest/LLoanInfoService/BatchListingStatusInfos"

  /**
    * 新版列表投标详情批量接口（请求列表大小不大于5）
    */
  val LOAN_BID_INFO_URL = "http://gw.open.ppdai.com/invest/LLoanInfoService/BatchListingBidInfos"

  /**
    * 自动登录接口，调用成功后返回： 用户名：UserName, 自动登录Token。
    * 拿到token后,拼接url:http://ac.ppdai.com/user/authcookie?token=XXXXX&jump=http://www.ppdai.com
    * 来将token种入用户的浏览器.其中token为接口中拿到的token,jump为种入token后需要用户跳转的页面
    */
  val AUTO_LOGIN_URL = "http://gw.open.ppdai.com/auth/LoginService/AutoLogin"

  /**
    * 获取用户投资列表的还款情况
    */
  val FETCH_LENDER_REPAYMENT_URL = "http://gw.open.ppdai.com/invest/RepaymentService/FetchLenderRepayment" //FetchLenderRepayment
}
