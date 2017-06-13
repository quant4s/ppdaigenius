package com.p2pgenius.restful

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{BidLog, PersistAction, PersistActionType, PersisterActor, PpdUserStrategy, Strategy}
import com.p2pgenius.ppdService.{ServiceAction, ServiceActionType}
import com.p2pgenius.strategies._
import com.p2pgenius.user._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}
import spray.http.HttpCookie
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
  val persistRef = actorRefFactory.actorSelection("/user/" + PersisterActor.path)
  implicit val timeout = Timeout(5 seconds)

  def userServiceRoute(implicit log: LoggingContext)  = {
    get {
      path ("ppd" / "login" / Rest) { username =>  // 拍拍贷用户登录， 绑定一个新用户
        parameters('code, 'state) { (code,state) =>
          invokeAuthize(code, username)
        }
      } ~
      path("api" / "strategies"/ Rest) {  ppdName => // 请求当前拍拍贷用户所有的策略列表
        complete{
          val future = spRef ? ServiceAction(ServiceActionType.FETCH_MY_STRATEGY_LIST, ppdName)
          val result1 = Await.result(future, timeout.duration).asInstanceOf[List[(Int, String, Int)]]
          log.debug("请求所有的系统策略%d".format(result1.size))

          val userRef = context.actorSelection("/user/%s/%s".format(UserManagerActor.path, ppdName))
          val future2 = userRef ? ServiceAction(ServiceActionType.FETCH_MY_STRATEGY_lIST_SETTING, ppdName)
          val result2 = Await.result(future2, timeout.duration).asInstanceOf[List[PpdUserStrategy]]
          log.debug("请求我的订阅%d".format(result2.size))

          log.debug("构建策略")
          val uIStrategies = new ArrayBuffer[UIStrategy]()
          for(r <- result1) {
            val s =  result2.find(f => f.sid == r._1).getOrElse(PpdUserStrategy(None, r._1, ppdName, 0, 58, 100000, 0 ))
            uIStrategies += UIStrategy(Some(r._1), r._2, "说明", r._3, s.status, s.amount, s.upLimit, s.start)
          }

          val json = Extraction.decompose(Result(0, "获取策略列表成功", uIStrategies))
          compact(render(json))
        }
      } ~
      path("api" / "strategy" / "bidLogs" / Rest / IntNumber ) { (ppdUser, sid) => // TODO 请求策略的投标日志
        complete {
          val ref = findUserActor(ppdUser)  // TODO:
          "bidlogs"
        }
      } ~
      path("api" / "bids" / Rest / IntNumber / IntNumber) { (ppdName, page, size) => // 请求所有:的投标日志
        complete {
          // val userRef = findUserActor(ppdName)  // TODO:
          val future = persistRef ? PersistAction(PersistActionType.FETCH_MY_BIDS, (ppdName, page, size))
          try {
            val result = Await.result(future, timeout.duration).asInstanceOf[List[BidLog]]

            val json = Extraction.decompose(Result(0, "成功", result))
            compact(render(json))
          } catch {
            case _ =>
              val json = Extraction.decompose(Result(1, "失败"))
              compact(render(json))
          }
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
      path("api" / "strategy" / Rest / IntNumber) { (ppdName, sid) => // 获取自定义策略详细信息
        complete {
          log.debug("获取策略自定义内容")
          val future = spRef ? ServiceAction(ServiceActionType.LOAD_STRATEGY_INFO, (ppdName, sid))
          val result1 = Await.result(future, timeout.duration).asInstanceOf[Result]
          val json = Extraction.decompose(result1)
          compact(render(json))
        }
      } ~
      path("api" / "recommended" / Rest) { ppdUser =>  // TODO: 推荐用户
        complete("")
      } ~
      path("api" / "ppdusers" / Rest) { username =>  // 关联账户
        complete{
          // val userRef = findUserActor(ppdName)
          val future = userManager ? ServiceAction(ServiceActionType.FETCH_MY_PPD_USER_LIST, username)
          val result1 = Await.result(future, timeout.duration).asInstanceOf[Result]

          val json = Extraction.decompose(result1)
          compact(render(json))
        }
      }
    } ~
    post {
      path("api" / "strategy" / Rest) { ppdName =>
        requestInstance { request =>
          complete {
            val json_str = request.entity.data.asString
            log.debug("新建一个策略，接收到数据%s".format(json_str))
           val jv = parse(json_str)
            val ss = jv.extract[StrategyDesc]
            // 提交给策略池管理器 保存数据到数据库
            val sid =  if(ss.id == 0) None else Some(ss.id)
            val strategy = new Strategy(sid, ss.name, ppdName, 3, json_str)

            val future = spRef ? ServiceAction(ServiceActionType.INSERT_STRATEGY, strategy)
            val result = Await.result(future, timeout.duration).asInstanceOf[Result]
            val json = Extraction.decompose(result)
            compact(render(json))
          }
        }
      } ~
      path ("api" / "users") {  // 用户注册
        requestInstance { request =>
          log.debug("新用户注册，接收到数据%s".format(request.entity.data.asString))
          val jv = parse(request.entity.data.asString)
          val user = jv.extract[UIUser]
          complete {
            val future = userManager ? ServiceAction(ServiceActionType.SIGN_UP, user)
            val result = Await.result(future, timeout.duration).asInstanceOf[Result]
            val json = Extraction.decompose(result)
            compact(render(json))
          }
        }
      } ~
      path("api" / "authenticate") {  // 用户登录
        requestInstance { request =>
          log.debug("用户认证%s".format(request.entity.data.asString))
          val jv = parse(request.entity.data.asString)
          val user = jv.extract[UIUser]
          complete {
            val future = userManager ? ServiceAction(ServiceActionType.SIGN_IN, user)
            val result = Await.result(future, timeout.duration).asInstanceOf[Result]
            val json = Extraction.decompose(result)
            compact(render(json))
          }

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
            if(ss.sub)  ref ! ServiceAction(ServiceActionType.SUB_STRATEGY, (ss.ppdName, ss.sid))
            else ref ! ServiceAction(ServiceActionType.UNSUB_STRATEGY, (ss.ppdName, ss.sid))

            val json = Extraction.decompose(Result(0, "订阅|取消订阅成功"))
            compact(render(json))
          }
        }
      } ~
      path("api" / "strategy" / "setting") {  // TODO 策略投资金额的设置
        requestInstance { request =>
          val jv = parse(request.entity.data.asString)
          val sa = jv.extract[UIStrategyAmount]
          log.debug("用户%s修改策略%d投资金额等信息".format(sa.ppdName, sa.sid))
          complete ("ok")
        }
      } ~
      path("api" / "setting") { ppdName =>   // TODO 修改起投利率, 自动投标
          requestInstance { request =>
          val jv = parse(request.entity.data.asString)
          val s = jv.extract[UISetting]
          log.debug("用户%s修改全局投标设置单笔投标金额:%d,起投利率:%d，保留金额:%d， 自动投标：%b"
            .format(s.ppdName, s.investAmount, s.startRate, s.reservedAmount, s.autoBid))
          complete("")
        }
      } ~
      path("api" / "strategy" / Rest) { ppdName =>  // 更新策略
        requestInstance { request =>
          log.debug("更新一个策略，接收到数据%s".format(request.entity.data.asString))
          complete {
            val json_str = request.entity.data.asString
           val jv = parse(json_str)
            val ss = jv.extract[StrategyDesc]
            // 提交给策略池管理器 保存数据到数据库
            val sid =  if(ss.id == 0) None else Some(ss.id)
            var strategy = new Strategy(sid, ss.name, ppdName, 3, json_str) // 3 表示自定义策略

            val future = spRef ? ServiceAction(ServiceActionType.UPDATE_STRATEGY, strategy)
            val result = Await.result(future, timeout.duration).asInstanceOf[Result]
            val json = Extraction.decompose(result)
            compact(render(json))
          }
        }
      }
    }~
    delete {
      path("api" / "strategy" / Rest/ IntNumber) {
        (ppdName, sid) => {
          complete {
            // 删除自定义策略
            val future = spRef ? ServiceAction(ServiceActionType.REMOVE_STRATEGY, (ppdName, sid))
            val result = Await.result(future, timeout.duration).asInstanceOf[Result]
            val json = Extraction.decompose(result)
            compact(render(json))
          }
        }
      }
    }
  }

  /**
    *
    * @param code
    * @param log
    * @return
    */
  def invokeAuthize(code: String, username: String)(implicit log: LoggingContext) = {
    val future = userManager ? ServiceAction(ServiceActionType.AUTHORIZE_USER, (code, username)) // AuthorizeUser(code, username)
    val result = Await.result(future, timeout.duration).asInstanceOf[Result]
    if (result.code == 0 ) {
      setCookie(HttpCookie("ppdUser", content = result.content.asInstanceOf[String])){
        log.debug("[invokeAuthize]授权成功，跳转到界面主界面")
        // getFromResource("dist/success.html")
        // redirect("index.html", StatusCodes.PermanentRedirect)
         complete("""<!DOCTYPE html><html lang="zh_CN"><head><meta http-equiv="refresh" content="2;url=/html"></head><body>3 秒后关闭</body></html>""")
      }
    }
    else
    // getFromResource("dist/success.html")
      complete("""<!DOCTYPE html><html lang="zh_CN"><head></head><body>授权失败，请重试</body></html>""")
   }

  def findUserActor(user: String) = context.actorSelection("/user/%s/%s".format(UserManagerActor.path, user))


}

case class UISubStrategy(sid: Int, ppdName: String, sub: Boolean)
case class UIStrategyAmount(sid: Int, ppdName: String, amount: Int, upLimit: Int, reservedAmount: Int)
case class UISetting(ppdName: String, investAmount: Int, startRate: Double, reservedAmount: Int, autoBid: Boolean)
case class UIStrategy(id: Option[Int], name: String, desc: String, kind: Int, status: Int, amount: Int, upLimit: Int, start: Int, sd: Option[StrategyDesc] = None)
case class UIUser(id: Option[Int], username: String, password: String, balance: Option[Double], grade: Option[Int])
