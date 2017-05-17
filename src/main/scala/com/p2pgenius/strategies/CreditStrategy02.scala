package com.p2pgenius.strategies

import com.p2pgenius.ppdService.LoanInfo

/**
  * Created by joe on 2017/5/7.
  */
class CreditStrategy02 extends UserStrategy {
  override val id: Int = 3
  override val name: String = "学历多借_01"

  override def check(loan: LoanInfo): Boolean = {
    (loan.Age >=28 && loan.Age <=40) &&
      (loan.Months >= 3 && loan.Months <= 12) &&
      (loan.Amount >= 1001 && loan.Amount <= 5000) &&
      Array("B","C","D").contains(loan.CreditCode) &&
      Array("博士","研究生","本科", "专升本").contains(loan.EducationDegree) &&
      "普通" == loan.StudyStyle &&
      loan.NormalCount >= 24 &&
      loan.OverdueLessCount <= 3 &&
      loan.OverdueMoreCount == 0 &&
      loan.OwingAmount < 500 &&     // 没有历史最高负债金额
       true                           // 平均逾期天数
      // TODO loan.LastBidTime// 距离上次借款天数
  }
}
