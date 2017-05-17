package com.p2pgenius.ppdService

import java.io.{File, PrintWriter}
import java.text.{DecimalFormat, DecimalFormatSymbols}

import dispatch.{url, _}
import Defaults._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinRouter
import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.util.{Failure, Success}


/**
  * 爬取拍拍贷网页借款页面，启动100个页面
  */
class CrawlPpdHtmlActor extends Actor with ActorLogging{
  val uri = "http://invest.ppdai.com/loan/info?id="
  override def receive: Receive = {
    case id: Long => crawlPage(id)
    case id: Int => {
      log.debug("获取")
      crawlPage(id)
    }
    case _ =>
  }

  /**
    * 爬取页面，并提取需要的数据
    * @param listingId
    */
  def crawlPage(listingId: Long): Unit = {
    val svc = url(uri + listingId)
    val req = Http(svc)

    val sender_old = sender
    req onComplete {
      case Success(resp) =>  {
        parsePage(resp.getResponseBody, listingId, sender_old)
        //Http.shutdown()
      }
      case Failure(ex) => println("Error")
    }
  }

  /**
    * 分析数据
    * @param html
    * @param listingId
    * @return
    */
  def parsePage(html: String,listingId: Long, sender_old: ActorRef) = {
    log.debug(html.substring(0,20))
    val  doc = Jsoup.parse(html)
    val elems: Elements = doc.select("p.ex")

    if(elems.size() >=15) {
      val size = elems.size()
      val x1 = elems.get(size-5).child(0).text().substring(1) // 累计借款金额
      val x2 = elems.get(size-2).child(0).text().substring(1) // 单笔最高借款金额
      val x3 = elems.get(size-1).child(0).text().substring(1) // 历史最高负债

      log.debug("分析页面数据成功%d, 发现元素：%d".format(listingId, elems.size()))
      sender_old.!(HtmlInfo(listingId, stringToDouble(x1), stringToDouble(x2),stringToDouble(x3)))(context.parent)
    } else {
      log.debug("分析页面数据失败%d, 发现元素：%d".format(listingId, elems.size()))
      sender_old.!("ERROR")(context.parent)
    }

//    val writer = new PrintWriter(new File("D:\\ppdai\\%d.html".format(listingId)))
//    writer.print(html)
//    writer.close()
  }

  def stringToDouble(s: String): Double = {
    val dfs = new DecimalFormatSymbols()
    dfs.setDecimalSeparator('.')
    dfs.setGroupingSeparator(',')
    dfs.setMonetaryDecimalSeparator('.')
    val df = new DecimalFormat("###,###.##",dfs)
    val num = df.parse(s)

    num.doubleValue()
  }

}

object CrawlPpdHtmlActor {
  def props() = {
    Props[CrawlPpdHtmlActor].withRouter(RoundRobinRouter(100))
  }

  val path = "crawl_html"
}

case class HtmlInfo(listingId: Long, sumLoanAmount: Double, maxLoanAmount:Double, highestDebt: Double)