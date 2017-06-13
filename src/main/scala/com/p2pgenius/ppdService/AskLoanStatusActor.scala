package com.p2pgenius.ppdService

import java.net.HttpURLConnection

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinRouter
import com.p2pgenius.strategies.StrategyPoolActor
import com.ppdai.open.{PropertyObject, ValueTypeEnum}
import com.ppdai.open.core.Result
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

/**
  * 处理请求状态的消息，返回结果
  */
class AskLoanStatusActor extends PpdRemoteService with Actor{

  override val connection: HttpURLConnection = createUrlConnection(LOAN_STATUS_URL)

  override def receive: Receive = {
    case PpdRemoteAction(PpdRemoteActionType.ASK_LOAN_LIST_STATUS,e) =>getLoanStatus(e.asInstanceOf[List[Long]])
    case e: Any => log.warning("[AskLoanStatusActor]不支持的消息%s".format(e.toString))
  }

  def getLoanStatus(ids: List[Long]) = {
    val json = ("ListingIds" -> ids)
    val result = send(createUrlConnection(LOAN_STATUS_URL), compact(render(json)) ,new PropertyObject("ListingIds", ids,ValueTypeEnum.Other))("")
    if(result != null) {
      val jv = parse(result.context)
      val loanListResult = jv.extract[LoanStatusResult]
      sender.!(loanListResult)(context.parent)
    } else
      sender.!(LoanStatusResult(null, -1, "", ""))(context.parent)
  }
}

object AskLoanStatusActor{
  def props() = Props[AskLoanStatusActor].withRouter(RoundRobinRouter(5))

  val path = "ask_loan_status"
}
