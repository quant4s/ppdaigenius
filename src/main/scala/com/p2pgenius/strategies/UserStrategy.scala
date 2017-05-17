package com.p2pgenius.strategies

import com.p2pgenius.persistence.Strategy
import com.p2pgenius.ppdService.{LoanInfo, LoanList}

/**
  * Created by joe on 2017/4/1.
  */
trait UserStrategy {

  val id: Int
  val name: String
  // 借款基本信息
  var ageMin = 0
  var ageMax = 99
  var gender = ""  // 男, 女, ""


  def check(loan: LoanInfo): Boolean = {
    var ret = false
    if(checkAge(loan) &&
      (checkSex(loan))) {
      ret = true
    }
    ret
  }

  def check(loan: LoanList) =  false

  def checkAge(loan:LoanInfo): Boolean = {
     true
  }

  def checkSex(loan:LoanInfo): Boolean = {
    if(gender == 0 || gender == 3) {
        true
    } else {
      gender == loan.Gender
    }
  }
}
