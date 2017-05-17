package com.p2pgenius.strategies

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.p2pgenius.persistence.Strategy
import com.p2pgenius.ppdService.{LoanInfo, LoanList}

import scala.collection.mutable.ArrayBuffer

/**
  * 策略执行器
  */
class StrategyActor(strategy: Strategy) extends Actor with ActorLogging{
  val subscribers = new ArrayBuffer[ActorRef]()
  val sd: StrategyDesc  = null

  override def receive: Receive = {
//    case FetchMyStrategies =>
    case s: SubscribeStrategy =>
      log.debug("有投资人订阅这个策略%s".format(strategy.name))
      subscribers += sender    // 加入投资人
    case s: UnSubscribeStrategy =>
      log.debug("有投资人退订这个策略%s".format(strategy.name))
      subscribers -= sender  // 删除投资人

    case li: LoanInfo =>
      log.debug("检测是否符合策略要求")
      if(check(li)){subscribers.foreach(f=> f ! LoanInfoWrapper(strategy,li))}  // 通知投资人投标
     case ll: LoanList =>
      log.debug("检测是否符合策略要求")
      //if(strategy.check(ll)) {subscribers.foreach(f=> f ! LoanListWrapper(strategy,ll))}  // 通知投资人投标
  }

  def check(li: LoanInfo): Boolean = {
    _checkAge(li) &&
    _checkSex(li) &&
    _checkMonth(li) &&
    _checkRate(li) &&
    _checkCreditCode(li) &&
    _checkTitle(li) &&
    _checkValidation(li) &&
    _checkEducationDegree(li) &&
    _checkStudyStyle(li) &&
    _checkSchool(li) &&
    _checkSuccessCount(li) &&
    _checkWasteCount(li) &&
    _checkCancelCount(li) &&
    _checkFailCount(li) &&
    _checkNormalCount(li) &&
    _checkOverdueLessCount(li) &&
    _checkOverdueMoreCount(li) &&
    _checkOwingAmount(li)
  }

  def _checkAge(li: LoanInfo) = {
    if(sd.ageMax == 0) true
    else (li.Age >= sd.ageMin && li.Age <= sd.ageMax)
  }

  def _checkSex(li: LoanInfo) = {
    if(!sd.male && !sd.female) true       // false false
    else if(sd.male && sd.female) true    // true true
    else if(sd.male) li.Gender == 1       // true false
    else  li.Gender == 2 // false true
  }

  def _checkMonth(li: LoanInfo) = {
    if(sd.monthMax == 0) true
    else (li.Months >= sd.monthMin && li.Months <= sd.monthMax)
  }

  def _checkRate(li: LoanInfo) = {
    if(sd.rateMax == 0) true
    else(li.CurrentRate >= sd.rateMin && li.CurrentRate <= sd.rateMax)
  }

  def _checkAmount(li: LoanInfo) = {
    if(sd.amountMax == 0) true
    else (li.Amount >= sd.amountMin && li.Amount <= sd.amountMax )
  }

  def _checkCreditCode(li: LoanInfo) = {
    !(sd.aaa || sd.aa || sd.a || sd.b || sd.c || sd.d || sd.e || sd.f ) ||  // 表示不检查
    (li.CreditCode == "AAA" && sd.aaa) ||
      (li.CreditCode == "AA" && sd.aa) ||
      (li.CreditCode == "A" && sd.a) ||
      (li.CreditCode == "B" && sd.b) ||
      (li.CreditCode == "C" && sd.c) ||
      (li.CreditCode == "D" && sd.d) ||
      (li.CreditCode == "E" && sd.e) ||
      (li.CreditCode == "F" && sd.f)
  }

  def _checkTitle(li: LoanInfo) = {
    true
  }

  def _checkValidation(li: LoanInfo) = {
    if(!(sd.videoValidate || sd.phoneValidate || sd.creditValidate ||
      sd.certificateValidate || sd.educateValidate || sd.nciicIdentityCheck)) // 表示不检查
      true
    else {
      if (sd.validateStyle) {
        // 满足所有认证
        __checkValidation(sd.videoValidate, li.VideoValidate) &&
          __checkValidation(sd.phoneValidate, li.PhoneValidate) &&
          __checkValidation(sd.creditValidate, li.CreditValidate) &&
          __checkValidation(sd.certificateValidate, li.CertificateValidate) &&
          __checkValidation(sd.educateValidate, li.EducateValidate) &&
          __checkValidation(sd.nciicIdentityCheck, li.NciicIdentityCheck)
      } else {
        __checkValidation(sd.videoValidate, li.VideoValidate) ||
          __checkValidation(sd.phoneValidate, li.PhoneValidate) ||
          __checkValidation(sd.creditValidate, li.CreditValidate) ||
          __checkValidation(sd.certificateValidate, li.CertificateValidate) ||
          __checkValidation(sd.educateValidate, li.EducateValidate) ||
          __checkValidation(sd.nciicIdentityCheck, li.NciicIdentityCheck)
      }
    }
  }

  def __checkValidation(need: Boolean, value: Int) = {
    if(need) value == 1
    else true
  }

  def _checkEducationDegree(li: LoanInfo) = {
    !(sd.educationDegreeBK || sd.educationDegreeBS || sd.educationDegreeYJS || sd.educationDegreeZSB ||
      sd.educationDegreeDZ || sd.educationDegreeZK || sd.educationDegreeQT)|| // 表示不检查
    __checkEducationDegree(sd.educationDegreeBK, li, "本科") ||
      __checkEducationDegree(sd.educationDegreeBS, li, "博士") ||
      __checkEducationDegree(sd.educationDegreeYJS, li, "研究生") ||
      __checkEducationDegree(sd.educationDegreeZSB, li, "专升本") ||
      __checkEducationDegree(sd.educationDegreeDZ, li, "大专") ||
      __checkEducationDegree(sd.educationDegreeZK, li, "专科") ||
      __checkEducationDegree(sd.educationDegreeQT, li, "其他")    // TODO: 其他的处理方式存在问题
  }

  def __checkEducationDegree(need: Boolean, li: LoanInfo, expected: String) = {
    if(need) li.EducationDegree == expected
    else false
  }

  def _checkStudyStyle(li: LoanInfo) = {
    !(sd.studyStylePT || sd.studyStyleCR || sd.studyStyleZK || sd.studyStyleQT) ||  // 表示不检查
    __checkStudyStyle(sd.studyStyleCR, li, "成人") ||
      __checkStudyStyle(sd.studyStyleZK, li, "自考") ||
      __checkStudyStyle(sd.studyStyleQT, li, "其他") ||
      __checkStudyStyle(sd.studyStylePT, li, "普通全日制")
  }

  def __checkStudyStyle(need:Boolean, li:LoanInfo, expected: String) = {
    if(need) li.StudyStyle == expected
    else false
  }

  def _checkSchool(li: LoanInfo) = {
    true    // TODO
  }

  def _checkSuccessCount(li: LoanInfo) = {
    if(sd.successCountMax == 0) true
    else (li.SuccessCount >= sd.successCountMin && li.SuccessCount <= sd.successCountMax)
  }

  def _checkWasteCount(li: LoanInfo) = {
    if(sd.wasteCountMax == 0) true
    else (li.WasteCount >= sd.wasteCountMin && li.WasteCount <= sd.wasteCountMax)
  }

  def _checkCancelCount(li: LoanInfo) = {
    if(sd.cancelCountMax == 0) true
    else (li.CancelCount >= sd.cancelCountMin && li.CancelCount <= sd.cancelCountMax)
  }

  def _checkFailCount(li: LoanInfo) = {
    if(sd.failCountMax == 0) true
    else (li.FailedCount >= sd.failCountMin && li.FailedCount <= sd.failCountMax)
  }

  def _checkNormalCount(li: LoanInfo) = {
    if(sd.normalCountMax == 0) true
    else (li.NormalCount >= sd.normalCountMin && li.NormalCount <= sd.normalCountMax)
  }

  def _checkOverdueLessCount(li: LoanInfo) = {
    if(sd.overdueLessCountMax == 0) true
    else (li.OverdueLessCount >= sd.overdueLessCountMin && li.OverdueLessCount <= sd.overdueLessCountMax)
  }

  def _checkOverdueMoreCount(li: LoanInfo) = {
    if(sd.overdueMoreCountMax == 0) true
    else (li.OverdueMoreCount >= sd.overdueMoreCountMin && li.OverdueMoreCount <= sd.overdueMoreCountMax)
  }

  def _checkOwingAmount(li: LoanInfo) = {
    if(sd.owingAmountMax == 0) true
    else(li.OwingAmount >= sd.owingAmountMin && li.OwingAmount <= sd.owingAmountMax)
  }
}

object StrategyActor {
  def props(strategy: Strategy) = Props(classOf[StrategyActor], strategy)
}

case class LoanInfoWrapper(strategy: Strategy, loanInfo: LoanInfo)
case class LoanListWrapper(strategy: Strategy, loanList: LoanList)
