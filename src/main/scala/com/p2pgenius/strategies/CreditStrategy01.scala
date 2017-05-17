package com.p2pgenius.strategies

import com.p2pgenius.ppdService.LoanInfo

/**
  * 博士策略， 魔镜BCD
  */
class CreditStrategy01 extends UserStrategy {
  override val id: Int = 2
  override val name: String = "博士策略"

  override def check(loan: LoanInfo): Boolean = {
    loan.EducationDegree == "博士" && Array("B","C","D").contains(loan.CreditCode)
  }
}
