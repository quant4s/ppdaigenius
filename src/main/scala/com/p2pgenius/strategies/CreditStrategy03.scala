package com.p2pgenius.strategies

import com.p2pgenius.ppdService.LoanInfo
import com.p2pgenius.restful.UIStrategy

/**
  * Created by joe on 2017/5/7.
  */
class CreditStrategy03 extends UserStrategy{
  override val id: Int = 4
  override val name: String = "重本首借_03"

  override def check(loan: LoanInfo): Boolean = {
    (loan.Age >=18 && loan.Age <=40) &&
      Array("B","C","D").contains(loan.CreditCode) &&
      Array("博士","研究生","本科").contains(loan.EducationDegree) &&
      "普通" == loan.StudyStyle &&
      true && // 211 985学校
      loan.SuccessCount == 0
  }
}
