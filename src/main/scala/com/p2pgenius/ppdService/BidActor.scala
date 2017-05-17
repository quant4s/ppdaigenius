package com.p2pgenius.ppdService


import java.net.HttpURLConnection

import akka.actor.{Actor, Props}
import akka.routing.RoundRobinRouter
import com.p2pgenius.persistence.{BidLog, PersisterActor, PpdUser, Strategy}
import com.ppdai.open.{PropertyObject, ValueTypeEnum}
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._


/**
  * 投标，并等待拍拍贷返回结果结果
  */
class BidActor extends Actor with PpdRemoteService {
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  override val connection: HttpURLConnection = createUrlConnection(BID_URL)

  override def receive: Receive = {
    case b: Bid => bid(b)
    case _ => log.warning("[BidActor]不支持的消息")
  }

  def bid(b: Bid): Unit = {
    val json = ("ListingId" -> b.listingId) ~ ("Amount" -> b.amount)
    val result = send(createUrlConnection(BID_URL), compact(render(json)),
      new PropertyObject("ListingId", b.listingId, ValueTypeEnum.Int32),
        new PropertyObject("Amount", b.listingId, ValueTypeEnum.Double))(b.ppdUser.accessToken)
    val jv = parse(result.context)
    val br = jv.extract[BidResult]

    var bidLog = BidLog(None, b.listingId, b.ppdUser.ppdName, b.strategy.id.getOrElse(0), b.strategy.name, br.Amount)
    // 保存到数据库
    persisRef ! bidLog
    // 把结果返回给投资人
    sender ! BidResultWrapper(b.strategy, b.ppdUser, br)
  }
}

object BidActor {
  def props() = {
    // Props(classOf[BidActor])
    Props[BidActor].withRouter(RoundRobinRouter(5))
  }

  val path = "bid"
}


case class Bid(strategy: Strategy, ppdUser: PpdUser, listingId: Long, amount: Int)
case class BidResultWrapper(strategy: Strategy, ppdUser: PpdUser, bidResult: BidResult)