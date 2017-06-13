package com.p2pgenius.ppdService

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * 拍拍贷远程服务接口管理，负责启动所有的接口
  * 1. 转发 获取借款信息
  * 2. 转发 获取借款状态
  * 3. 转发 获取借款投资人信息
  * 4. 转发 投标信息 Bid
  */
class PpdRemoteManagerActor extends Actor with ActorLogging{
  implicit val timeout = Timeout(5 seconds)

  val loanInfoRouter = context.actorOf(AskLoanInfoActor.props(), AskLoanInfoActor.path)
  val loanStatusRouter = context.actorOf(AskLoanStatusActor.props(), AskLoanStatusActor.path)
  val bidRouter = context.actorOf(BidActor.props(), BidActor.path)
  val htmlRouter = context.actorOf(CrawlPpdHtmlActor.props(), CrawlPpdHtmlActor.path)
  val ref1 = context.actorOf(AskLoanListActor.props(), AskLoanListActor.path)

  override def receive: Receive = {
    case m: PpdRemoteAction if(m.action == PpdRemoteActionType.ASK_LOAN_LIST_INFO) => loanInfoRouter forward m
    case m: PpdRemoteAction if(m.action == PpdRemoteActionType.ASK_LOAN_LIST_STATUS) => loanStatusRouter forward m
    case m: PpdRemoteAction if(m.action == PpdRemoteActionType.FETCH_HTML_INFO) => htmlRouter forward m
    case m: PpdRemoteAction if(m.action == PpdRemoteActionType.BID) => bidRouter forward m

//    case  RemoteService(ActionType.LOAN_INFO, e) => {
    //      log.debug("转发获取借款消息")
    //      val future = loanInfoRouter ? e
    //      val sender_old = sender
    //      future onSuccess {
    //        case r: LoanInfoResult => sender_old ! r
    //      }
    //    }

//    case RemoteService(ActionType.LOAN_STATUS, e) => {
    //      log.debug("转发获取状态消息")
    //      val future = loanStatusRouter ? e
    //      val sender_old = sender
    //      future onSuccess {
    //        case r: LoanStatusResult => sender_old ! r
    //      }
    //    }

//    case  RemoteService(ActionType.LOAN_BIDER, e) =>
//    case RemoteService(ActionType.LOAN_BID, e) => bidRouter forward e
//    case RemoteService(ActionType.HTML_INFO, e) => {
//      log.debug("转发获取页面消息")
//      val future = htmlRouter ? e
//      val sender_old = sender
//      future onSuccess {
//        case r: HtmlInfo => {
//          log.debug("转发获取页面结果")
//          sender_old ! r
//        }
//        case s: String  => {
//          log.debug("转发获取页面结果,表示分析错误的字符串")
//          sender_old ! s
//        }
//      }
//      future onFailure {
//        case ex: Any => log.error("[PpdRemoteManagerActor][HTML_INFO]发现错误%s".format(ex.getMessage))
//      }
//    }

    case m: Any=> log.debug("接收到不支持的消息%s".format(m.toString))
  }

}

object PpdRemoteManagerActor {
  def props() = {
    Props(classOf[PpdRemoteManagerActor])
  }

  val path = "ppd_remote_manager"
}

//case class RemoteService(action: ActionType, body: Any)
//
//object ActionType extends Enumeration {
//  type ActionType = Value
//  val LOAN_LIST,
//    LOAN_INFO,
//  LOAN_STATUS,
//  LOAN_BID,
//  LOAN_BIDER,
//  HTML_INFO = Value
//}