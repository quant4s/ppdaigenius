package com.p2pgenius

import akka.actor.ActorSystem
import akka.event.Logging
import akka.io.IO
import com.p2pgenius.persistence.PersisterActor
import com.p2pgenius.ppdService.PpdRemoteManagerActor
import com.p2pgenius.strategies.StrategyPoolActor
import com.p2pgenius.user.UserManagerActor
import com.ppdai.open.core.{OpenApiClient, PropertyObject, ValueTypeEnum}
import com.typesafe.config.ConfigFactory
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods._
import spray.can.Http

/**
 * @author ${user.name}
 */
object MainApp extends App{


//  val ids = Array[Int](10000, 100001)
//  val json = OpenApiClient.propertyToJson(new PropertyObject("ListingId", 9575229, ValueTypeEnum.Int32),
//    new PropertyObject("ids", ids, ValueTypeEnum.Other),
//    new PropertyObject("Amount",150, ValueTypeEnum.Double))
//
//  println(json)

//  val json1 = ("ListingId" -> 95758) ~  ("AMOUNT" -> 10)
//  println(compact(render(json1)))

  implicit val system = ActorSystem("ppdai", ConfigFactory.load)
  val log = Logging.getLogger(system,this)
  log.info("启动持久层")
  system.actorOf(PersisterActor props, PersisterActor path)

  log.info("启动拍拍贷服务接口")
  system.actorOf(PpdRemoteManagerActor props, PpdRemoteManagerActor path)

  log.info("启动用户管理Actor，并启动所有的用户Actor")
  system.actorOf(UserManagerActor props, UserManagerActor path)

  log.info("启动策略管理Actor，启动所有的策略Actor")
  system.actorOf(StrategyPoolActor props, StrategyPoolActor path)

  log.debug("启动HTTP服务器")
  val handler = system.actorOf(HttpHandler.props, HttpHandler.path)
  val interface = "localhost" // system.settings.config.getString("app.interface")
  val port = 8080 //system.settings.config.getInt("app.port")
  IO(Http) ! Http.Bind(handler, interface, port)

//  def propertyToJson(propertyObjects: com.ppdai.open.PropertyObject*): String = {
//    implicit val formats = DefaultFormats
//    val json = propertyObjects.map { obj =>
//      (obj.lowerName -> obj.value.toString)
//    }
//    compact(render(json))
//  }
//
//  case class Winner(id: Long, numbers: List[Int])
//  case class Lotto(id: Long, winningNumbers: List[Int], winners: List[Winner], drawDate: Option[java.util.Date])
//
//  val winners = List(Winner(23, List(2, 45, 34, 23, 3, 5)), Winner(54, List(52, 3, 12, 11, 18, 22)))
//  val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5, 3), winners, None)
//
//  var json2 = lotto.winners.map { w =>
//      (w.id.toString -> w.numbers) }
//
//  println(compact(render(json2)))
}
