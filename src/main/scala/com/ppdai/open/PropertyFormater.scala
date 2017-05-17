package com.ppdai.open

import java.nio.ByteBuffer
import java.text.{ParseException, SimpleDateFormat}
import java.util.{Date, UUID}

import com.ppdai.open.ValueTypeEnum.ValueTypeEnum
import com.ppdai.open.ValueTypeEnum._
import org.apache.commons.codec.binary.Hex
/**
  * Created by joe on 2017/4/20.
  */
object PropertyFormater {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  /**
    * 对象格式化
    *
    * @param name      属性名称
    * @param value     属性值
    * @param valueType 属性类型
    * @return 格式化后的字符创
    */
  def ObjectFormat(name: String, value: Any, valueType: ValueTypeEnum): String = {
    var formatValue: Any =  valueType match {
      case DateTime =>  dateTimeFormat(value)
      case Single =>  floatFormat(value)
      case Double =>  doubleFormat(value)
      case Decimal =>  decimalFormat(value)
      case Boolean =>  booleanFormat(value)
      case Guid =>  guidFormat(value)
      case SByte =>  value
      case Int16 => value
      case Int32 => value
      case Int64 =>  value
      case Byte => value
      case UInt16 =>  value
      case UInt32 =>  value
      case UInt64 =>  value
      case Char =>  value
      case String =>  value
      case Other => null
    }
    if (formatValue == null) ""
    else "%s%s".format(name, formatValue)
  }

  /**
    * 日期时间格式化
    *
    * @param obj 待格式化对象
    * @return
    * @throws ParseException
    */
  @throws[ParseException]
  def dateTimeFormat(obj: Any): Long = {
    val real = if (obj.isInstanceOf[Date])  obj.asInstanceOf[Date] else sdf.parse(obj.toString)
    (real.getTime - sdf.parse("1970-01-01").getTime) / 1000
  }

  /**
    * 浮点格式化
    *
    * @param obj 待格式化对象
    * @return
    */
  def floatFormat(obj: Any): String = {
    var real = if (obj.isInstanceOf[Float]) obj.asInstanceOf[Float] else obj.toString.toFloat
    Hex.encodeHexString(toByteArray(real)).toUpperCase
  }

  /**
    * 双精度格式化
    *
    * @param obj 待格式化对象
    * @return
    */
  def doubleFormat(obj: Any): String = {
    val real = if (obj.isInstanceOf[Double]) obj.asInstanceOf[Double] else obj.toString.toDouble
    Hex.encodeHexString(toByteArray(real)).toUpperCase
  }

  /**
    * 布尔格式化
    *
    * @param obj 待格式化对象
    * @return
    */
  def booleanFormat(obj: Any): Int = {
    val real = if (obj.isInstanceOf[Boolean]) obj.asInstanceOf[Boolean] else obj.toString.toBoolean
    if (real) 1 else 0
  }

  /**
    * Decimal格式化
    *
    * @param obj 待格式化对象
    * @return
    */
  def decimalFormat(obj: Any): String = doubleFormat(obj)

  /**
    * Guid格式化
    *
    * @param obj 待格式化对象
    * @return
    */
  def guidFormat(obj: Any): String = {
    val real =  if (obj.isInstanceOf[UUID]) obj.asInstanceOf[UUID] else  UUID.fromString(obj.toString)
    real.toString
  }

  /**
    * 浮点类型转换成字节数组
    *
    * @param value
    * @return
    */
  def toByteArray(value: Float): Array[Byte] = {
    val bytes = new Array[Byte](4)
    ByteBuffer.wrap(bytes).putFloat(value)
    bytes
  }

  /**
    * 双精度类型转换成字节数组
    *
    * @param value
    * @return
    */
  def toByteArray(value: Double): Array[Byte] = {
    val bytes: Array[Byte] = new Array[Byte](8)
    ByteBuffer.wrap(bytes).putDouble(value)
    return bytes
  }
}
