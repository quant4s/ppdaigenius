package com.ppdai.open

import java.text.ParseException

import com.ppdai.open.ValueTypeEnum.ValueTypeEnum

/**
  * Created by joe on 2017/4/20.
  */
class PropertyObject(val name: String, val value: Any, val valueType: ValueTypeEnum) {
  val lowerName = name.toLowerCase

  def isSign: Boolean = value != null && (valueType ne ValueTypeEnum.Other)

  override def toString: String = {
    try
      return PropertyFormater.ObjectFormat(lowerName, value, valueType)

    catch {
      case e: ParseException => {
        e.printStackTrace()
      }
    }
    null
  }
}
