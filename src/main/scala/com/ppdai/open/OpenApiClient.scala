//package com.ppdai.open
//
//import java.io._
//import java.net.{HttpURLConnection, MalformedURLException, ProtocolException, URL}
//import java.text.SimpleDateFormat
//import java.util.Date
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.databind.node.ArrayNode
//import com.ppdai.open.PKCSType.PKCSType
//
///**
//  * Created by joe on 2017/4/20.
//  */
//object OpenApiClient {
//  /**
//    * 获取授权信息URL
//    */
//  private val AUTHORIZE_URL = "https://ac.ppdai.com/oauth2/authorize"
//  /**
//    * 刷新Token信息URL
//    */
//  private val REFRESHTOKEN_URL = "https://ac.ppdai.com/oauth2/refreshtoken "
//  private val dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//  private var appid: String = null
//  private var rsaCryptoHelper: RsaCryptoHelper = null
//
//  @throws[Exception]
//  def Init(appid: String, pkcsTyps: PKCSType, publicKey: String, privateKey: String) {
//    OpenApiClient.appid = appid
//    rsaCryptoHelper = new RsaCryptoHelper(pkcsTyps, publicKey, privateKey)
//  }
//
//  /**
//    * 向拍拍贷网关发送请求
//    * @param url 网关地址
//    * @param propertyObjects 参数对象
//    * @return
//    */
//  def send(url: String, propertyObjects: PropertyObject*): Result = send(url, 1, null, propertyObjects: _* )
//
//  /**
//    * 向拍拍贷网关发送请求
//    * @param url
//    * @param version
//    * @param propertyObjects
//    * @return
//    */
//  def send(url: String, version: Double, propertyObjects: PropertyObject*): Result = send(url, version, null, propertyObjects: _*)
//
//  /**
//    * 向拍拍贷网关发送请求
//    *
//    * @param url
//    * @param accessToken
//    * @param propertyObjects
//    * @return
//    */
//  def send(url: String, accessToken: String, propertyObjects: PropertyObject*): Result = send(url, 1, accessToken, propertyObjects: _*)
//
//  /**
//    * 向拍拍贷网关发送请求
//    *
//    * @param url
//    * @param accessToken
//    * @param propertyObjects
//    * @return
//    */
//  def send(url: String, version: Double, accessToken: String, propertyObjects: PropertyObject*): Result = {
//    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
//    var result: Result = Result()
//    try{
//      val serviceUrl = new URL(url)
//      val urlConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
//      urlConnection.setDoInput(true)
//      urlConnection.setDoOutput(true)
//      urlConnection.setUseCaches(false)
//      /** ************ OpenApi所有的接口都只提供Post方法 **************/
//      urlConnection.setRequestMethod("POST")
//      urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
//      urlConnection.setRequestProperty("X-PPD-SIGNVERSION", "1")
//      urlConnection.setRequestProperty("X-PPD-SERVICEVERSION", String.valueOf(version))
//      /** ***************** 公共请求参数 ************************/
//      urlConnection.setRequestProperty("X-PPD-APPID", appid)
//      //获取UTC时间作为时间戳
//      val cal = java.util.Calendar.getInstance
//      val zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET)
//      val dstOffset = cal.get(java.util.Calendar.DST_OFFSET)
//      cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset))
//      val timestamp = (cal.getTime.getTime - dateformat.parse("1970-01-01 00:00:00").getTime) / 1000
//      urlConnection.setRequestProperty("X-PPD-TIMESTAMP", timestamp.toString)
//      //对时间戳进行签名
//      urlConnection.setRequestProperty("X-PPD-TIMESTAMP-SIGN", rsaCryptoHelper.sign(appid + timestamp).replaceAll("\\r", "").replaceAll("\\n", ""))
//      val sign = rsaCryptoHelper.sign(ObjectDigitalSignHelper.getObjectHashString(propertyObjects)).replaceAll("\\r", "").replaceAll("\\n", "")
//      urlConnection.setRequestProperty("X-PPD-SIGN", sign)
//      if (accessToken != null && !"" == accessToken) urlConnection.setRequestProperty("X-PPD-ACCESSTOKEN", accessToken)
//      /** ************************************************************/
//      val dataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
//      dataOutputStream.writeBytes(propertyToJson(propertyObjects))
//      dataOutputStream.flush()
//      val inputStream = urlConnection.getInputStream
//      val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
//      val bufferedReader = new BufferedReader(inputStreamReader)
//      val strResponse = bufferedReader.readLine
//      result.sucess = true
//      result.context = strResponse
//
//    catch {
//      case e: UnsupportedEncodingException => {
//        e.printStackTrace()
//        result.errorMessage = e.getMessage
//      }
//      case e: ProtocolException => {
//        e.printStackTrace()
//        result.errorMessage = e.getMessage
//      }
//      case e: MalformedURLException => {
//        e.printStackTrace()
//        result.errorMessage = e.getMessage
//      }
//      case e: IOException => {
//        e.printStackTrace()
//        result.errorMessage = e.getMessage
//      }
//      case e: Exception => {
//        e.printStackTrace()
//        result.errorMessage = e.getMessage
//      }
//    } finally {
//    }
//    result
//  }
//
//  /**
//    * @param propertyObjects
//    * @return
//    */
//  private def propertyToJson(propertyObjects: PropertyObject*) = {
//    val mapper = new ObjectMapper
//    val node = mapper.createObjectNode
//    for (propertyObject <- propertyObjects) {
//      if (propertyObject.value.isInstanceOf[Integer]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[Integer])
//      else if (propertyObject.value.isInstanceOf[Long]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[Long])
//      else if (propertyObject.value.isInstanceOf[Float]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[Float])
//      else if (propertyObject.value.isInstanceOf[BigDecimal]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[BigDecimal])
//      else if (propertyObject.value.isInstanceOf[Double]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[Double])
//      else if (propertyObject.value.isInstanceOf[Boolean]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[Boolean])
//      else if (propertyObject.value.isInstanceOf[String]) node.put(propertyObject.name, propertyObject.value.asInstanceOf[String])
//      else if (propertyObject.value.isInstanceOf[Date]) node.put(propertyObject.name, dateformat.format(propertyObject.value.asInstanceOf[Date]))
//      else if (propertyObject.value.isInstanceOf[util.Collection[_]]) {
//        val arrayNode = mapper.createArrayNode
//        node.put(propertyObject.name, arrayNode)
//        val it = propertyObject.value.asInstanceOf[util.Collection[_]].iterator
//        if (!it.hasNext) break //todo: break is not supported
//        while (true) {
//          val e = it.next
//          addArrayNode(arrayNode, e)
//          if (!it.hasNext) break //todo: break is not supported
//        }
//      }
//      else node.put(propertyObject.name, propertyObject.value.toString)
//    }
//    mapper.writeValueAsString(node)
//  }
//
//  private def addArrayNode(arrayNode: ArrayNode, value: Any) {
//    if (value.isInstanceOf[Integer]) arrayNode.add(value.asInstanceOf[Integer])
//    else if (value.isInstanceOf[Float]) arrayNode.add(value.asInstanceOf[Float])
//    else if (value.isInstanceOf[Long]) arrayNode.add(value.asInstanceOf[Long])
//    else if (value.isInstanceOf[Double]) arrayNode.add(value.asInstanceOf[Double])
//    else if (value.isInstanceOf[BigDecimal]) arrayNode.add(value.asInstanceOf[BigDecimal])
//    else if (value.isInstanceOf[String]) arrayNode.add(value.asInstanceOf[String])
//    else if (value.isInstanceOf[Boolean]) arrayNode.add(value.asInstanceOf[Boolean])
//    else throw new IllegalArgumentException("不支持的类型")
//  }
//
//  /**
//    * 获取授权
//    *
//    * @param code 授权码
//    * @return
//    * @throws IOException
//    */
//  @throws[Exception]
//  def authorize(code: String): AuthInfo = {
//    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
//    val serviceUrl = new URL(AUTHORIZE_URL)
//    val urlConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
//    urlConnection.setDoInput(true)
//    urlConnection.setDoOutput(true)
//    urlConnection.setUseCaches(false)
//    urlConnection.setRequestMethod("POST")
//    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
//    val dataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
//    /** ****************** 获取授权参数 AppID code *********************/
//    dataOutputStream.writeBytes(String.format("{\"AppID\":\"%s\",\"code\":\"%s\"}", appid, code))
//    dataOutputStream.flush()
//    val inputStream = urlConnection.getInputStream
//    val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
//    val bufferedReader = new BufferedReader(inputStreamReader)
//    val strResponse = bufferedReader.readLine
//    val mapper = new ObjectMapper
//    mapper.readValue(strResponse, classOf[AuthInfo])
//  }
//
//  /**
//    * 刷新AccessToken
//    *
//    * @param openId       用户OpenID
//    * @param refreshToken 刷新Token
//    * @return
//    * @throws IOException
//    */
//  @throws[Exception]
//  def refreshToken(openId: String, refreshToken: String): AuthInfo = {
//    if (appid == null || "" == appid) throw new Exception("OpenApiClient未初始化")
//    val serviceUrl = new URL(REFRESHTOKEN_URL)
//    val urlConnection = serviceUrl.openConnection.asInstanceOf[HttpURLConnection]
//    urlConnection.setDoInput(true)
//    urlConnection.setDoOutput(true)
//    urlConnection.setUseCaches(false)
//    urlConnection.setRequestMethod("POST")
//    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
//    val dataOutputStream = new DataOutputStream(urlConnection.getOutputStream)
//    /** **************** 刷新Token参数 AppID OpenID RefreshToken **********************/
//    dataOutputStream.writeBytes(String.format("{\"AppID\":\"%s\",\"OpenID\":\"%s\",\"RefreshToken\":\"%s\"}", appid, openId, refreshToken))
//    dataOutputStream.flush()
//    val inputStream = urlConnection.getInputStream
//    val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
//    val bufferedReader = new BufferedReader(inputStreamReader)
//    val strResponse = bufferedReader.readLine
//    val mapper = new ObjectMapper
//    mapper.readValue(strResponse, classOf[AuthInfo])
//  }
//
//  def getRsaCryptoHelper: RsaCryptoHelper = {
//    return rsaCryptoHelper
//  }
//}
