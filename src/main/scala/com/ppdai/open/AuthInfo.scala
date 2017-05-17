package com.ppdai.open

/**
  * Created by joe on 2017/4/20.
  */
case class AuthInfo(OpenID: String, AccessToken: String, RefreshToken: String, ExpiresIn: String)

case class Result(var sucess: Boolean = false, var context: String = "", var errorMessage: String = "")

object PKCSType extends Enumeration {
  type PKCSType = Value
  val PKCS1, PKCS8 = Value
}

object ValueTypeEnum extends Enumeration {
  type ValueTypeEnum = Value
  val DateTime,
  Single,
  Double,
  Decimal,
  Boolean,
  SByte,
  Int16,
  Int32,
  Int64,
  Byte,
  UInt16,
  UInt32,
  UInt64,
  Char,
  String,
  Guid,
  Other = Value
}