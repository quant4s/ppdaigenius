package com.ppdai.open

import java.util.Collections

import scala.collection.mutable.ArrayBuffer

/**
  * Created by joe on 2017/4/20.
  */
object ObjectDigitalSignHelper {
  def getObjectHashString(propertyObjects: PropertyObject*): String = {
//    val list = new util.ArrayList[String]
//    propertyObjects.sortBy(e => e.isSign).reduce(_.lowerName + _.lowerName)
 // t
    try {
      val sb = StringBuilder.newBuilder
      propertyObjects.sortBy(po => po.lowerName).foreach { o =>
        sb.append(o.lowerName)
        sb.append(o.value)
      }
      sb.toString()
    } catch {
      case ex: Exception =>
        println("exception is raised " + ex.getMessage)
        ""
    }
//    var list = List[String]
//    for (propertyObject <- propertyObjects) {
//      if (propertyObject.isSign) list += propertyObject.lowerName //list.add(propertyObject.lowerName)
//    }


//    Collections.sort(list)
//    val sb = new StringBuffer
//    import scala.collection.JavaConversions._
//    for (ln <- list) {
//      for (propertyObject <- propertyObjects) {
//        if (ln == propertyObject.lowerName) {
//          sb.append(propertyObject.toString)
//          break //todo: break is not supported
//        }
//      }
//    }
//    sb.toString
  }
}
