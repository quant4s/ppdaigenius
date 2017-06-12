package com.p2pgenius.ppdService

import java.net.{HttpURLConnection, URL}

import akka.actor.{Actor, ActorLogging, Props}
import akka.actor.Actor.Receive
import akka.routing.{FromConfig, RoundRobinRouter}
import com.p2pgenius.persistence.PersisterActor
import com.p2pgenius.strategies.StrategyPoolActor
import com.ppdai.open.{PropertyObject, ValueTypeEnum}
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * 请求借款详细信息
  */
class AskLoanInfoActor extends Actor with PpdRemoteService with ActorLogging {
//  val spRef = context.actorSelection("/user/%s".format(StrategyPoolActor.path))
//  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  override val connection: HttpURLConnection = createUrlConnection(LOAN_INFO_URL)

  override def preStart(): Unit = {
    log.debug("启动AskLoanInfoActor")
    super.preStart()
  }

  def askLoanInfoActor(listIds: List[Long]) = {
//    log.debug("请求借款详情")
    val json = ("ListingIds" -> listIds)
    val result = send(createUrlConnection(LOAN_INFO_URL), compact(render(json)), new PropertyObject("ListingIds", listIds,ValueTypeEnum.Other))("")
    if(result != null) {
      log.debug("请求借款详情" + result.context)
      val jv = parse(result.context)
      val loanInfos = jv.extract[LoanInfoResult]
      sender.!(loanInfos)(context.parent)
    } else {
      sender.!(LoanInfoResult(null, -1, "", ""))(context.parent)
    }
  }

  override def receive: Receive = {
    case ids: List[Long] ⇒ {
      log.debug("处理请求借款详细信息的请求")
      askLoanInfoActor(ids)
    }
    case m: Any ⇒ {
      log.warning("不被支持的消息%s".format(m.toString))
    }
  }
}

object AskLoanInfoActor {
  def props() = {
    Props[AskLoanInfoActor].withRouter(RoundRobinRouter(5))
  }

  val path = "ask_loan_info"
}
