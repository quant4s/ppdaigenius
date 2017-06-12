package com.p2pgenius.strategies

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{PersistAction, PersistActionType, PersisterActor, PpdUser, Strategy}
import com.p2pgenius.ppdService.{LoanInfo, LoanList}
import com.p2pgenius.restful.UIStrategy
import com.p2pgenius.user.Result

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * 策略池
  * 1. 创建一个新的自定义策略
  * 2. 删除一个自定义策略
  * 3. 创建一个策略缓冲池
  */
class StrategyPoolActor extends Actor with ActorLogging {
  private var strategyActorRefs = new mutable.HashMap[Int, ActorRef]()
  private var strategyMap = new mutable.HashMap[Int, Strategy]()    // 仅仅保存用户策略
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  implicit val timeout = Timeout(5 seconds)

  context.system.scheduler.scheduleOnce( 2 seconds, self, "INIT")

  override def receive: Receive = {
    case "INIT" => init()

    case s: Strategy => saveStrategyDesc(s)

    case FetchMyStrategies(ppdName) => fetchMyStrategies(ppdName)

    case FetchStrategyInfo(ppdName, sid) => {
      val s = strategyMap.get(sid)
      if(s == None)
        sender ! Result(1, "没有找到策略")
      else
        sender ! Result(0, "成功加载策略", s.get)
    }


    case s: SubscribeStrategy => {
      log.debug("转发消息到订阅的策略")
      strategyActorRefs(s.sid) forward s
    }
    case s: UnSubscribeStrategy => strategyActorRefs(s.sid) forward s

    case s: DeleteUserStratgy => removeUserDefineStrategy(s)

    case li: LoanInfo =>
      log.debug("转发借款标的到策略LoanInfo")
      strategyActorRefs.foreach(f => f._2 forward li)
    case ll: LoanList =>
      log.debug("转发借款标的到策略LoanList")
      strategyActorRefs.foreach(f => f._2 forward ll)

    case x: Any => log.warning("[StrategyPoolActor]不支持的消息" + x.toString)
  }

  /**
    * 初始化，启动多个策略
    */
  def init(): Unit = {
    log.debug("从数据库中读取所有的策略数据")
    val future = persisRef ? PersistAction(PersistActionType.FETCH_ALL_STRATEGIES)
    future onSuccess {
      case strategies: List[Strategy]  => {
        for(s <- strategies) {
          strategyMap += (s.id.getOrElse(0) -> s)
        }
        initStrategies()
      }
    }
    future onFailure {
      case e: Exception => log.debug("读取失败")
    }

//    val s = new HighRateStrategy(6)
//    val ref = context.actorOf(StrategyActor.props(s))
//    strategyActorRefs += (s.id -> ref)
//    strategyMap += (s.id -> Strategy(s.id, s.name, "", 2, ""))
//    log.debug("初始化策略%s".format(s.name))
  }

  def initStrategies(): Unit = {
    for(s <- strategyMap) {
      val ref = context.actorOf(StrategyActor.props(s._2), s._1.toString)
      strategyActorRefs += (s._1 -> ref)
      log.debug("初始化策略%s".format(s._2.name))
    }
  }

//  def fetchStrategies(): Unit = {
//    sender ! strategyMap.values.toList
//  }

  def saveStrategyDesc(s: Strategy): Unit = {
    val sender_old = sender
    if(s.id != None && strategyMap.get(s.id.get) == None){
      sender_old ! Result(1,"非法策略")
    } else {
      val future = persisRef ? PersistAction(PersistActionType.INSERT_OR_UPDATE_STRATEGY, s)
      future onSuccess {
        case strategy: Strategy  => {
          log.debug("[StrategyPoolActor.saveStrategyDesc] 成功保存")
          // 修改本地策略缓存
          if(strategyMap.contains(strategy.id.get)) strategyMap(strategy.id.get) = strategy
          else strategyMap += (strategy.id.get -> strategy)
          sender_old ! Result(0,"成功保存")
        }
      }
      future onFailure {
        case e: Exception => log.debug("读取失败")
      }


    }
  }

  /**
    * 移除自定义策略
    * @param s
    */
  def removeUserDefineStrategy(s: DeleteUserStratgy): Unit = {
    strategyActorRefs(s.sid) ! PoisonPill
    strategyActorRefs.remove(s.sid)

    // TODO: 从数据库中逻辑删除
  }

  /**
    * 获取我的所有策略
    * @param ppdName
    */
  def fetchMyStrategies(ppdName: String): Unit = {
    log.debug("返回所有的系统策略和我的%s私有策略%d".format(ppdName, strategyMap.size))
    var ss = List[(Int, String, Int)]()
    strategyMap.foreach( m => {
      val s = m._2
      if(s.ppdName == "" || s.ppdName == ppdName) {
        ss = ss :+ (s.id.get, s.name, s.kind)
      }
    })

    sender ! ss

  }


}

object StrategyPoolActor {
  def props() = Props(classOf[StrategyPoolActor])

  val path = "strategy_pool"
}

case class SubscribeStrategy(ppdUser: PpdUser, sid: Int)
case class UnSubscribeStrategy(ppdUser: PpdUser, sid: Int)
//case class FetchStrategies()
case class FetchMyStrategies(ppdName: String)
case class DeleteUserStratgy(ppdName: String, sid: Int)
case class StrategiesDataArrived()

case class FetchStrategyInfo(ppdName: String, sid: Int)

