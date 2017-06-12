package com.p2pgenius.ppdService

import java.net.HttpURLConnection

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import com.p2pgenius.persistence.{Overdue, PersistAction, PersistActionType, PersisterActor}
import com.ppdai.open.{PropertyObject, ValueTypeEnum}
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.{DefaultFormats, Extraction, Formats}


/**
  * 多长时间查询一次， 数据源， 结果是什么
  */
class FetchLenderRepaymentActor extends Actor with PpdRemoteService with ActorLogging{
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))

  override def receive: Receive = ???

  override val connection: HttpURLConnection = createUrlConnection(FETCH_LENDER_REPAYMENT_URL)

  def getListingIdList(): Unit = {
    // todo: 检查逾期标是否已经还款
//    val innerJoin = for {
//      (c, s) <- coffees join suppliers on (_.supID === _.id)
//    } yield (c.name, s.name)
    // todo: 检查
  }

  def fetchLenderRepayment(listingId: Long, token: String): Unit = {
    val json = ("ListingId" -> listingId)
    val result = send(createUrlConnection(FETCH_LENDER_REPAYMENT_URL), compact(render(json)),
      new PropertyObject("ListingId", listingId,ValueTypeEnum.Int32))(token)

    val jv = parse(result.context)
    val payments = jv.extract[ListingPaymentResult]

    // 逾期标 找出最大逾期天数
    var maxDays  = 0
    var count = 0
    for( p <- payments.ListingPayment) {
      if((p.RepayStatus == 0 || p.RepayStatus == 4) && p.OverdueDays > maxDays) {
        maxDays = p.OverdueDays
        count += 1
      }
    }

    if(maxDays > 0) { // 保存逾期数据库
      persisRef ! PersistAction(PersistActionType.INSERT_OR_UPDATE_OVERDUE, Overdue(listingId, maxDays, count))
    }

    // todo: 更新LoanInfo 中的数据
  }
}
