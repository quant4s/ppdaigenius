package com.p2pgenius.restful

import java.util.Date

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{PpdUser, PpdUserStrategy, Strategy}
import com.p2pgenius.strategies._
import com.p2pgenius.user._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}
import spray.http.{HttpCookie, StatusCodes}
import spray.routing.HttpService
import spray.util.LoggingContext

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * 1. 一个新的用户授权
  */
trait UserService extends HttpService { this: Actor ⇒
  implicit def json4sFormats: Formats = DefaultFormats

  val userManager = actorRefFactory.actorSelection("/user/" + UserManagerActor.path)
  val spRef = actorRefFactory.actorSelection("/user/" + StrategyPoolActor.path)
  implicit val timeout = Timeout(5 seconds)
//  implicit val formats: Formats = DefaultFormats

  def userServiceRoute(implicit log: LoggingContext)  = {
    get {
      path ("ppd" / "login") {  // 用户登录， 绑定一个新用户
        parameters('code) { code =>
          invokeAuthize(code)
        }

      } ~
      path("api" / "strategies"/ Rest) {  ppdName => // 请求所有的策略列表
        complete{
          val future = spRef ? FetchMyStrategies(ppdName)
          val result1 = Await.result(future, timeout.duration).asInstanceOf[List[(Int, String, Int)]]
          log.debug("请求所有的系统策略%d".format(result1.size))

          val userRef = context.actorSelection("/user/%s/%s".format(UserManagerActor.path, ppdName))
          val future2 = userRef ? FetchMyStrategies(ppdName)
          val result2 = Await.result(future2, timeout.duration).asInstanceOf[List[PpdUserStrategy]]
          log.debug("请求我的订阅%d".format(result2.size))

          log.debug("构建策略")
          val uIStrategies = new ArrayBuffer[UIStrategy]()
          for(r <- result1) {
            val s =  result2.find(f => f.sid == r._1).getOrElse(PpdUserStrategy(None, r._1, ppdName, 0, 58, 100000, 0 ))
            uIStrategies += UIStrategy(r._1, r._2, "说明", r._3, s.status, s.amount, s.upLimit, s.start)
          }

          val json = Extraction.decompose(uIStrategies)
          compact(render(json))
        }
      } ~
      path("api" / "strategy" / "bidLogs" / Rest / IntNumber ) { (ppdUser, sid) => // 请求策略的投标日志
        complete {
          "bidlogs"
        }
      } ~
      path("api" / "bids" / Rest) { ppdUser => // 请求所有的投标日志
        complete {
          val userRef = findUserActor(ppdUser)
          //userRef ! FetchMyBids()
          "bids"
        }
      } ~
      path("api"  / "info" / Rest) { ppdUser => // 基本信息
        complete {
          "infos"
        }
      } ~
      path("api"/ "flow"  / Rest ) { ppdUser => // 流量记录
        complete {
          "flow"
        }
      } ~
      path("api" / "strategy" / Rest / IntNumber) { (ppdUser, sid) => // 获取自定义策略详细信息
        complete {
          ""
        }
      } ~
      path("api"  / "accounts"/ Rest) { ppdUser =>  // 获取其他相关的账户
        complete("")
      } ~
      path("api" / "recommended" / Rest) { ppdUser =>  // 推荐用户
        complete("")
      } ~
      path("api" / "ppdusers" / Rest) { ppdName =>  // 关联账户
        complete{
          // val userRef = findUserActor(ppdName)
          val future = userManager ? FetchMyRelativePpdUsers(ppdName)
          val result1 = Await.result(future, timeout.duration).asInstanceOf[List[PpdUser]]

//          val pu1 = PpdUser("1", 10000.45, 1, 58, 0, 18, "fewfef", "fewfewfw", "grewhgfg", 34500, new Date(), 1)
//          val pu2 = PpdUser("pdu2345", 10000.45, 1, 58, 0, 18, "fewfef", "fewfewfw", "grewhgfg", 34500, new Date(), 1)
//          val pu3 = PpdUser("pdu3456", 10000.45, 1, 58, 0, 18, "fewfef", "fewfewfw", "grewhgfg", 34500, new Date(), 1)
//          val xx =  List(pu1, pu2, pu3)
          val json = Extraction.decompose(result1)
          compact(render(json))
//          """
//            |[{"ppdName":"pdu1234","balance":10000.45,"uid":1,"investAmount":58,"reserveAmount":0,"startRate":18.0,"status":1},
//            |{"ppdName":"pdu2345","balance":10000.45,"uid":1,"investAmount":58,"reserveAmount":0,"startRate":18.0,"status":1},
//            |{"ppdName":"pdu3456","balance":10000.45,"uid":1,"investAmount":58,"reserveAmount":0,"startRate":18.0,"status":1}]
//          """.stripMargin
//          log.debug("接受到用户%s请求关联账户,返回%s".format(ppdUser, s));
//          s

        }
      }
    } ~
    post {
      path("api" / "strategy" / Rest) { ppdName =>
        requestInstance { request =>
          log.debug("新建一个策略，接收到数据%s".format(request.entity.data.asString))
          complete("ok")
        }
      }
    } ~
    put {
      path("api" / "strategy" / "sub" ) { // 用户名， 策略编号， 订阅 0：false， !0:true
        requestInstance { request =>
          complete {
            val jv = parse(request.entity.data.asString)
            val ss = jv.extract[UISubStrategy]
            log.debug("用户%s订阅策略%d----%b".format(ss.ppdName, ss.sid, ss.sub))
            val ref = findUserActor(ss.ppdName)
            if(ss.sub)  ref ! SubStrategy(ss.ppdName, ss.sid)
            else ref ! UnsubStrategy(ss.ppdName, ss.sid)
            "ok"
          }
        }
      } ~
      path("api" / "strategy" / "amount") {
        requestInstance { request =>
          val jv = parse(request.entity.data.asString)
          val sa = jv.extract[UIStrategyAmount]
          log.debug("用户%s修改策略%d投资金额等信息".format(sa.ppdName, sa.sid))
          complete ("ok")
        }
      } ~
      path("api" / "setting") { ppdName =>   // 修改起投利率，保留金额，单笔投标金额, 自动投标
        requestInstance { request =>
          val jv = parse(request.entity.data.asString)
          val s = jv.extract[UISetting]
          log.debug("用户%s修改全局投标设置单笔投标金额:%d,起投利率:%d，保留金额:%d， 自动投标：%b"
            .format(s.ppdName, s.investAmount, s.startRate, s.reservedAmount, s.autoBid))
          complete("")
        }
      } ~
      path("api" / "strategy" / Rest) { ppdName =>
        requestInstance { request =>
          log.debug("新建一个策略，接收到数据%s".format(request.entity.data.asString))
          complete {
            val jv = parse(request.entity.data.asString)
            val ss = jv.extract[UIStrategy]
            // 提交给策略池管理器 保存数据到数据库
            //val strategy =  Strategy(Some(ss.id). ss.)
            spRef ! ss

            "OK"
//            complete("ok")
        }
      }
    }
    delete {
      path("api" / "strategy" / Rest/ IntNumber) {
        (ppdName, sid) => {
        // 删除自定义策略
        //spRef ! DeleteUserStratgy(ppdName, sid)
          complete{
            "delete"
          }
        }
      }
    }
  }

  def invokeAuthize(code: String)(implicit log: LoggingContext) = {
    val future = userManager ? AuthorizeUser(code)
    val result = Await.result(future, timeout.duration).asInstanceOf[String]
    if (result != "ERROR") {
      setCookie(HttpCookie("ppdUser", content = result)){
        log.debug("跳转到界面主界面")
        redirect("/index.html", StatusCodes.PermanentRedirect)
        // complete("""<!DOCTYPE html><html lang="zh_CN"><head><meta http-equiv="refresh" content="2;url=/html"></head><body>正在跳转</body></html>""")
      }
    }
    else
    complete("""<!DOCTYPE html><html lang="zh_CN"><head></head><body>授权失败，请重试</body></html>""")
   }

  def findUserActor(user: String) = context.actorSelection("/user/%s/%s".format(UserManagerActor.path, user))


}

case class UISubStrategy(sid: Int, ppdName: String, sub: Boolean)
case class UIStrategyAmount(sid: Int, ppdName: String, amount: Int, upLimit: Int, reservedAmount: Int)
case class UISetting(ppdName: String, investAmount: Int, startRate: Double, reservedAmount: Int, autoBid: Boolean)

case class UIStrategy(id: Int, name: String, desc: String, kind: Int, status: Int, amount: Int, upLimit: Int, start: Int, sd: Option[StrategyDesc] = None)
