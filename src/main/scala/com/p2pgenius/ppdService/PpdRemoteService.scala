package com.p2pgenius.ppdService

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import com.ppdai.open._
import org.json4s._

import scala.concurrent.duration._

/**
  * 负责拍拍贷接口调用
  */
trait PpdRemoteService extends ActorLogging { this: Actor ⇒

  val autoLoginUrl = new URL(AUTO_LOGIN_URL)
  implicit val timeout = Timeout(2 seconds)
  implicit val formats = DefaultFormats
  val rsaCryptoHelper = new RsaCryptoHelper(PKCSType.PKCS8,SERVER_PUBLIC_KEY, CLIENT_PRIVATED_KEY)
  val connection: HttpURLConnection

  def createUrlConnection(url: String, version: String = "1") = {
    val serviceUrl = new URL(url)
    val urlConnection = serviceUrl.openConnection().asInstanceOf[HttpURLConnection]
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(true)
    urlConnection.setUseCaches(false)

    urlConnection.setRequestMethod("POST")
    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
    urlConnection.setRequestProperty("X-PPD-SIGNVERSION", "1")
    urlConnection.setRequestProperty("X-PPD-SERVICEVERSION", version)
    urlConnection.setRequestProperty("X-PPD-APPID", APP_ID)

    urlConnection
  }

  def send(httpURLConnection: HttpURLConnection, json: String, accessToken: String, propertyObjects: PropertyObject*): Result= {
    send(httpURLConnection, json, propertyObjects: _*)(accessToken)
  }

  def send(httpURLConnection: HttpURLConnection, json: String, propertyObjects: PropertyObject*)(accessToken: String)= {
    if (accessToken != null && !"".equals(accessToken))
      httpURLConnection.setRequestProperty("X-PPD-ACCESSTOKEN", accessToken)

    //获取UTC时间作为时间戳
    val timestamp = new Date().getTime() /1000
    httpURLConnection.setRequestProperty("X-PPD-TIMESTAMP", timestamp.toString())
    //对时间戳进行签名
    httpURLConnection.setRequestProperty("X-PPD-TIMESTAMP-SIGN", rsaCryptoHelper.sign(APP_ID + timestamp).replaceAll("\\r", "").replaceAll("\\n", ""))

     val  data = ObjectDigitalSignHelper.getObjectHashString(propertyObjects: _*)
    httpURLConnection.setRequestProperty("X-PPD-SIGN", rsaCryptoHelper.sign(data).replaceAll("\\r", "").replaceAll("\\n", ""))

    var result: Result = null.asInstanceOf[Result]
    try {
      httpURLConnection.connect()
      // 数据流
      val dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream())
//      dataOutputStream.writeBytes(propertyToJson(propertyObjects: _*))
      dataOutputStream.writeBytes(json)
      dataOutputStream.flush()
      val inputStream = httpURLConnection.getInputStream()
      val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
      val bufferedReader = new BufferedReader(inputStreamReader)
      val strResponse = bufferedReader.readLine()

      result = Result(true, strResponse, "")
      httpURLConnection.disconnect()
    }
    catch {
      case ex: Exception => log.error(ex.getMessage)
    }
    result
//    if(result.sucess) {
//      val jv = parse(result.context)
//      jv.extract[T]
//    } else
//      null.asInstanceOf[T]
  }

//  def propertyToJson(propertyObjects: PropertyObject*): String = {
//    val json = ("cc" -> propertyObjects.map { obj =>
//      ((obj.lowerName -> obj.value))
//    }).asInstanceOf[JValue]
//    compact(render(json))
//  }



}
