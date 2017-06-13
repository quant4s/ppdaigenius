package com.p2pgenius.ppdService

import java.net.{HttpURLConnection, URL}

import akka.actor.{Actor, ActorLogging, Props}
import akka.actor.Actor.Receive
import akka.routing.{FromConfig, RoundRobinRouter}
import akka.util.Timeout
import com.p2pgenius.persistence.PersisterActor
import com.p2pgenius.strategies.StrategyPoolActor
import com.ppdai.open.{PropertyObject, ValueTypeEnum}
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import scala.concurrent.duration._
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

  def askLoanInfo(listIds: List[Long]) = {
//    log.debug("请求借款详情")
    val json = ("ListingIds" -> listIds)
    log.debug("开始请求详情%s".format(listIds.toString()))
    val result = send(createUrlConnection(LOAN_INFO_URL), compact(render(json)), new PropertyObject("ListingIds", listIds,ValueTypeEnum.Other))("")
    log.debug("结束请求详情%s".format(listIds.toString()))
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
    case PpdRemoteAction(PpdRemoteActionType.ASK_LOAN_LIST_INFO, ids) => askLoanInfo(ids.asInstanceOf[List[Long]])

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
