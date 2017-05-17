package com.p2pgenius.restful

import spray.http.StatusCodes
import spray.routing.HttpService
import spray.util.LoggingContext
import  com.p2pgenius.ppdService._

/**
  * Created by joe on 2017/4/27.
  */
trait StaticResourcesService extends HttpService{
  def staticServiceRoute(implicit log: LoggingContext)  = {
    get {
      path("") {
        optionalCookie("ppdUser") {
          case Some(nameCookie) =>
            log.debug("跳转到界面主界面")
            redirect("/html/", StatusCodes.PermanentRedirect)
          case None =>
            log.debug("跳转到授权登录页面")
            val authUrl = "https://ac.ppdai.com/oauth2/login?AppID=%s&ReturnUrl=%s".format(APP_ID, LOGIN_CALLBACK_URL)
            redirect(authUrl, StatusCodes.TemporaryRedirect)
        }
      }~
      path("index.html" ) {
       getFromResource("index.html")
      } ~
      pathPrefix("main.bundle.js") {
        getFromResource("main.bundle.js")
      } ~
      pathPrefix("inline.bundle.js") {
        getFromResource("inline.bundle.js")
      } ~
      pathPrefix("polyfills.bundle.js") {
        getFromResource("polyfills.bundle.js")
      } ~
      pathPrefix("styles.bundle.js") {
        getFromResource("styles.bundle.js")
      } ~
      pathPrefix("html" / "js") {
        getFromResourceDirectory("html/js/")
      }
    }
  }
}
