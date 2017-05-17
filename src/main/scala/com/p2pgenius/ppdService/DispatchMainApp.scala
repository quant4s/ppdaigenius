package com.p2pgenius.ppdService

import java.io.File

import dispatch._
import Defaults._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.util.{Failure, Success}


/**
  * Created by joe on 2017/5/9.
  */
object DispatchMainApp extends App{

  def parsePage(html: String): Unit = {
    val input = new File("C:/Users/joe/Desktop/new2.html");
    val doc = Jsoup.parse(input, "UTF-8", "http://ppdai.com/");
//    val  doc = Jsoup.parse(html)
    //val elems: Elements = doc.select("p.ex")
    val elems: Elements = doc.select("p.tab-hd")
    val elem = elems.get(2)

    val els = elem.select("span.num")
    val x1 = els.get(0).text()
    val x2 = els.get(3).text()
    val x3 = els.get(4).text()
//    val x1 = elems.get(11).child(0).text().substring(1)  // 累计借款金额
//    val x2 = elems.get(14).child(0).text().substring(1)  // 单笔最高借款金额
//    val x3 = elems.get(15).child(0).text().substring(1)  // 历史最高负债
    println("%s   %s    %s".format(x1, x2, x3))

    val elems2 = doc.select("table.lendDetailTab_tabContent_table1")
    println(elems2.get(1).select("td").html())
    println(elems2.get(2).select("td").html())
  }

  val svc = url("http://invest.ppdai.com/loan/info?id=%d".format(36819364))
  val req = Http(svc)
  //val resp = req()
//  println (resp.getStatusCode)
  //println ("返回的HTML 是：" + resp.getResponseBody)
  req onComplete {
    case Success(s) =>  {
      parsePage(s.getResponseBody)
      Http.shutdown()
    }
    case Failure(ex) => println("Error")
  }





//  req onSuccess { case s =>
//    println(s.getResponseBody)
//  }
//  req onFailure { case ex =>
//    println(ex.getMessage)
//  }

//  for (str <- html) println(str)

}
