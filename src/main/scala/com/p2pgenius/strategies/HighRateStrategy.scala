package com.p2pgenius.strategies
import com.p2pgenius.ppdService.LoanInfo


/**
  * 高息赔标，>= rate 指定利率的标都投
  * @param rate
  */
class HighRateStrategy(rate: Double) extends UserStrategy {
  override val id: Int = 1
  override val name = rate + "以上赔"

  override def check(loan: LoanInfo): Boolean = {
    loan.CurrentRate >= rate
  }
}
