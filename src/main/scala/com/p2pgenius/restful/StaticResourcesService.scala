package com.p2pgenius.restful

import spray.http.StatusCodes
import spray.routing.HttpService
import spray.util.LoggingContext
import com.p2pgenius.ppdService._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._


/**
  * Created by joe on 2017/4/27.
  */
trait StaticResourcesService extends HttpService{
  def staticServiceRoute(implicit log: LoggingContext)  = {
    get {
      path("") {
//        optionalCookie("ppdUser") {
//          case Some(nameCookie) =>
//         getFromResource("dist/index.html")
              redirect("index.html", StatusCodes.PermanentRedirect)

        //          case None =>
//            log.debug("跳转到授权登录页面")
//            val authUrl = "https://ac.ppdai.com/oauth2/login?AppID=%s&ReturnUrl=%s".format(APP_ID, LOGIN_CALLBACK_URL)
//            redirect(authUrl, StatusCodes.TemporaryRedirect)
//        }
      }~
      path("index.html" ) {
        //complete(index)
        log.debug("访问index.html,")
        getFromResource("dist/index.html")
//        redirect("", StatusCodes.PermanentRedirect)
      } ~
      path("config.json") {
        complete {
          val json = ("callbackUrl" -> LOGIN_CALLBACK_URL) ~~ ("appId" -> APP_ID) ~~ ("loginUrl" -> PPD_LOGIN_URL)
          compact(render(json))
//          """{
//            |  "callbackUrl": "http://localhost:8080/ppd/login/",
//            |  "appId": "094e0ffd6cca46429e7c51751f1f0a3e",
//            |  "loginUrl":"https://ac.ppdai.com/oauth2/login?"
//            |}
//          """.stripMargin
        }
      } ~
      pathPrefix("main.bundle.js") {
        getFromResource("dist/main.bundle.js")
      } ~
      pathPrefix("inline.bundle.js") {
        getFromResource("dist/inline.bundle.js")
      } ~
      pathPrefix("polyfills.bundle.js") {
        getFromResource("dist/polyfills.bundle.js")
      } ~
      pathPrefix("styles.bundle.js") {
        getFromResource("dist/styles.bundle.js")
      } ~
      pathPrefix("html" / "js") {
        getFromResourceDirectory("html/js/")
      }
    }
  }

  val index =
    """
      |<!doctype html>
      |<html>
      |<head>
      |  <meta charset="utf-8">
      |  <title>赢在拍拍</title>
      |  <base href="/">
      |
      |  <meta name="viewport" content="width=device-width, initial-scale=1">
      |  <link rel="icon" type="image/x-icon" href="favicon.ico">
      |  <script src="//cdn.bootcss.com/jquery/2.1.4/jquery.min.js" language="JavaScript" ></script>
      |  <script src="//cdn.bootcss.com/bootstrap/3.3.4/js/bootstrap.min.js"></script>
      |
      |  <link href="//cdn.bootcss.com/bootstrap/3.3.4/css/bootstrap.min.css" rel="stylesheet">
      |  <link href="//cdn.bootcss.com/bootstrap/3.3.4/css/bootstrap-theme.min.css" rel="stylesheet">
      |  <link href="//cdn.bootcss.com/bootstrap-switch/3.3.4/css/bootstrap2/bootstrap-switch.min.css" rel="stylesheet">
      |  <script src="//cdn.bootcss.com/bootstrap-switch/3.3.4/js/bootstrap-switch.min.js"></script>
      |  <script language="javascript">
      |    var ppduser=
      |  </script>
      |</head>
      |<body class="container" style="width: 1120px; ">
      |  <app-root>Loading...</app-root>
      |<script type="text/javascript" src="inline.bundle.js"></script><script type="text/javascript" src="polyfills.bundle.js"></script><script type="text/javascript" src="styles.bundle.js"></script><script type="text/javascript" src="main.bundle.js"></script></body>
      |</html>
      |
    """.stripMargin
}
