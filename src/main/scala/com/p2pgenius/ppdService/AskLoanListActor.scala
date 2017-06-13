package com.p2pgenius.ppdService

import java.net.{HttpURLConnection, URL}
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import com.p2pgenius.persistence.{PersistAction, PersistActionType, PersisterActor}
import com.p2pgenius.strategies.StrategyPoolActor
import com.ppdai.open.{PropertyObject, Result, ValueTypeEnum}
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * 维护投标中借款列表，当借款列表状态不为投标中的时候， 从维护列表中移除，每30秒维护一次
  * 1. 每间隔指定的时间，就请求一次借款列表
  * 2. 每间隔指定的时间，就请求状态  异步
  * 3. 请求详细借款信息 异步
  * 4. 实时保存到数据库
  */
class AskLoanListActor extends Actor with PpdRemoteService with ActorLogging {
  val ppdServiceRef = context.actorSelection("/user/%s".format(PpdRemoteManagerActor.path))
  val spRef = context.actorSelection("/user/%s".format(StrategyPoolActor.path))
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))

  var loanMap = new mutable.HashMap[Long, (String, Int, LoanInfo, HtmlInfo)]()   //
  var lastBidTime: Date = null

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("启动AskLoanListActor")
    context.system.scheduler.schedule(1 second, 1000 milliseconds, self, "ASK_LOAN_LIST")
    context.system.scheduler.schedule(2 second, 1000 milliseconds, self, "ASK_LOAN_STATUS")
//    context.system.scheduler.schedule(35 seconds, 30 seconds, self, "CLEAR_COLLECTION")

    super.preStart()
  }

  override val connection: HttpURLConnection = createUrlConnection(LOAN_LIST_URL)


  /**
    * 获取借款列表
    *
    * @return
    */
  def askLoanList() = {
    log.debug("请求远程接口，获取借款列表")
    val json = ("PageIndex" -> 1)
    var result: Result = null
    if(lastBidTime != null) {
      json ~ ("StartDateTime" -> "")
      result = send(createUrlConnection(LOAN_LIST_URL), compact(render(json)),
        new PropertyObject("PageIndex", 1, ValueTypeEnum.Int32))(null)
    } else {
//      json ~ ("StartDateTime" -> "")
      result = send(createUrlConnection(LOAN_LIST_URL), compact(render(json)),
        new PropertyObject("PageIndex", 1, ValueTypeEnum.Int32),
        new PropertyObject("StartDateTime", "", ValueTypeEnum.DateTime))(null)
    }
    lastBidTime = new Date()

    // 请求调用借款详细信息
    try {
      log.debug(result.context)
      val jv = parse(result.context)
      val loanListResult = jv.extract[LoanListResult]
      var list =  List[Long]()

      // 加入新的借款列表
      for(l <- loanListResult.LoanInfos){
        if(!checkLoanExists(l)) {
          loanMap += (l.ListingId -> (l.Title, 2,  null, null))
          list = list :+ l.ListingId
        }
      }

      // 抓取页面数据
//      for(id <- list) {
      //        fetchHtmlData(id)
      //      }

      // 请求借款详细信息
      // var list = loanMap.keys.toList
      while(list.size > 0 && list != null) {
        list = fetchLoanInfo(list)
      }

//      for (loanList <- loanListResult.LoanInfos) {
//        log.debug("请求调用借款详情")
//        // 如果为高息赔标，那么直接开始抢标 FIXME: 下面的13 需要参数化
//        if (loanList.CreditCode == "AA" && loanList.Rate >= 13) {
//          // 发送到策略过滤器 过滤
//          spRef ! loanList
//        }
//      }
    } catch {
      case ex: Exception => log.error(ex.getMessage)
    }
  }

  /**
    * 获取N个借款信息的数组, 异步访问
    * @param list
    * @param n
    * @return
    */
  def fetchLoanInfo(list: List[Long], n: Int = 10) = {
    val a = list.splitAt(n)
    log.debug("请求借款信息%d".format(a._1.size))
    _internalFetchLoanInfo(a._1)
    a._2
  }

  private def _internalFetchLoanInfo(list: List[Long]): Unit = {
    val future = ppdServiceRef ? PpdRemoteAction(PpdRemoteActionType.ASK_LOAN_LIST_INFO, list)
    future onSuccess {
      case result: LoanInfoResult => {
        if(result.Result == 1) {
          for (li <- result.LoanInfos) {
            if (loanMap.get(li.ListingId) == None)
              log.warning("[AskLoanListActor.fetchLoanInfo]Map中不存在这个Key，请检查%d".format(li.ListingId))
            else {
              // 构建
              log.debug("[AskLoanListActor.fetchLoanInfo]详细借款信息%d".format(li.ListingId))

              loanMap(li.ListingId) = loanMap(li.ListingId).copy(_3 = li)

              log.debug("保存到数据库%d".format(li.ListingId))
              persisRef ! PersistAction(PersistActionType.INSERT_LOAN, loanMap(li.ListingId))

              log.debug("提交到策略池")
              spRef ! li
            }
          }
        } else {
          context.system.scheduler.scheduleOnce(100 milliseconds, self, AskLoanListActor.ActionType("INTERNAL_FETCH_LOAN_INFO", list))
//          _internalFetchLoanInfo(list)
        }
      }
      case s: Any => log.warning("[AskLoanListActor.fetchLoanInfo]不支持的消息%s".format(s.toString))
    }
    future onFailure {
      case ex: Any => {
//        _internalFetchLoanInfo(list)
        context.system.scheduler.scheduleOnce(100 milliseconds, self, AskLoanListActor.ActionType("INTERNAL_FETCH_LOAN_INFO", list))
        log.error("[AskLoanListActor.fetchLoanInfo]发生错误%s".format(ex.getMessage))
      }
    }
  }

  /**
    * 请求状态
    */
  def askLoanStatus(list: List[Long], n: Int = 10) = {
    val a = list.splitAt(n)
    _internalAskLoanStatus(a._1)
    a._2
  }

  private def _internalAskLoanStatus(list: List[Long]): Unit = {
    val future = ppdServiceRef ? PpdRemoteAction(PpdRemoteActionType.ASK_LOAN_LIST_STATUS, list)  // FIXME: 状态
    future onSuccess {
      case ls: LoanStatusResult => {
        log.debug("[askLoanStatus]得到请求的返回状态")
        if(ls.Result != 1) {
          // 不成功， 重新查询
          log.debug("[_internalAskLoanStatus]查询状态不成功， 重新查询,错误%s".format(ls.ResultMessage))
          context.system.scheduler.scheduleOnce(1 second, self, AskLoanListActor.ActionType("INTERNAL_ASK_LOAN_STATUS", list))
          //_in ternalAskLoanStatus(list)
        } else {
          ls.Infos.foreach(sr => {
            loanMap(sr.ListingId) = loanMap(sr.ListingId).copy(_2 = sr.Status)
            if (Array(0, 3, 4, 5).contains(sr.Status)) {
              // 0 :流标  3 :借款成功（成功 || 成功已还清） 4: 审核失败 5 :撤标
              // 从内存中移走, TODO: 更新数据库的状态
              log.debug("该标%d已经完成，从内存中移走".format(sr.ListingId))
              loanMap -= sr.ListingId
            }
          })
        }
      }
    }
    future onFailure {
      case ex: Any => {
        context.system.scheduler.scheduleOnce(1 second, self, AskLoanListActor.ActionType("INTERNAL_ASK_LOAN_STATUS", list))
        log.error("[AskLoanListActor.askLoanStatus]请求%s状态, 发生错误 %s. 已经重新查询".format(list.toString(), ex.getMessage))
      }
    }
  }

  /**
    * 请求 HTML 页面数据
    */
  def fetchHtmlData(listingId: Long): Unit = {
    log.debug("开始抓取页面信息%d".format(listingId))
    val future = ppdServiceRef ? PpdRemoteAction(PpdRemoteActionType.FETCH_HTML_INFO, listingId)
    future onSuccess {
      case hi: HtmlInfo => {
        log.debug("已经抓取到页面信息%d".format(listingId))
        if(loanMap.get(hi.listingId) == None)
          log.warning("[AskLoanListActor.fetchHtmlData]Map中不存在这个Key，请检查%d".format(hi.listingId))
        else {  // 构建
          log.debug("[AskLoanListActor.fetchHtmlData]详细借款信息%d".format(hi.listingId))

          loanMap(hi.listingId) = loanMap(hi.listingId).copy(_4 = hi)

          log.debug("保存到数据库%d".format(hi.listingId))
          persisRef ! hi
        }
      }
      case _ => log.warning("没有抓取到页面信息%d".format(listingId))
    }
    future onFailure {
      case ex: Any => log.error("[AskLoanListActor.fetchHtmlData]发生错误%s".format(ex.getMessage))
    }
  }

  def clearCollection(): Unit = {
    val list = loanMap.values.toList
    for( v <- list) {
      if(Array(0,3,4,5).contains(v._2)) { // 0 :流标  3 :借款成功（成功 || 成功已还清） 4: 审核失败 5 :撤标
        // 从内存中移走
        log.debug("从MAP中移走数据");
        loanMap -= v._3.ListingId
      }
    }
  }

  override def receive: Receive = {
    case "ASK_LOAN_LIST" => askLoanList()
    case "ASK_LOAN_STATUS" => {
      val list = loanMap.keys.toList.take(10)
      _internalAskLoanStatus(list)
//      var list = loanMap.keys.toList
//      while(list.size > 0 && list != null) {
//        list = askLoanStatus(list)
//      }
    }
    case "CLEAR_COLLECTION" => clearCollection()

    case AskLoanListActor.ActionType("INTERNAL_ASK_LOAN_STATUS", list) => _internalAskLoanStatus(list.asInstanceOf[List[Long]])
    case AskLoanListActor.ActionType("INTERNAL_FETCH_LOAN_INFO", list) => _internalFetchLoanInfo(list.asInstanceOf[List[Long]])


    case m: Any => log.warning("不支持的消息%s".format(m.toString))
  }

  private def checkLoanExists(loan: LoanList): Boolean = loanMap.contains(loan.ListingId)

}

object AskLoanListActor {
  def props() = {
    Props(classOf[AskLoanListActor])
  }

  val path = "ask_loan_list"

  private case class ActionType(_type: String, body: Any = null)
}
