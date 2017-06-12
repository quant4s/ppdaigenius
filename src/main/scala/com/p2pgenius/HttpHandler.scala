package com.p2pgenius

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRefFactory, Props}
import com.p2pgenius.restful.{StaticResourcesService, UserService}
import spray.http.HttpHeaders.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`, `Access-Control-Max-Age`}
import spray.routing.RejectionHandler.Default
import spray.routing.{ExceptionHandler, RejectionHandler, RoutingSettings}
import spray.routing.directives.RespondWithDirectives.respondWithHeaders
import spray.routing.directives.MethodDirectives.options
import spray.routing.directives.RouteDirectives.complete
import spray.http.HttpMethods.{DELETE, GET, OPTIONS, POST, PUT}
import spray.http.{AllOrigins, StatusCodes}
import spray.util.LoggingContext

/**
  * Created by joe on 2017/4/17.
  */
class HttpHandler extends Actor with UserService with StaticResourcesService with ActorLogging{
  private val allowOriginHeader = `Access-Control-Allow-Origin`(AllOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Authorization"),
    `Access-Control-Max-Age`(1728000)) //20 days


  def receive = runRoute( respondWithHeaders(`Access-Control-Allow-Methods`(OPTIONS, GET, POST, DELETE, PUT) ::  allowOriginHeader :: optionsCorsHeaders){
    optionsRoute ~ userServiceRoute  ~ staticServiceRoute
  })

  val optionsRoute = {
    options {
      complete(StatusCodes.OK)
    }
  }

  def actorRefFactory = context
  def systemRef = context.system
  implicit def executionContext = actorRefFactory.dispatcher
}

object HttpHandler {
  def props() = {
    Props(classOf[HttpHandler])
  }

  val path = "http_handler"
}
