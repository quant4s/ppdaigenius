package com.p2pgenius
import java.util.Date

import com.typesafe.config.ConfigFactory

import scala.slick.driver.{H2Driver, JdbcProfile, MySQLDriver, PostgresDriver}
import scala.slick.lifted.ProvenShape


/**
  * Created by joe on 2017/4/17.
  */
package object persistence {
  def getDriver() = {
    val TEST = "test"
    val DEV = "dev"
    val PROD = "prod"
    val mode = "dev" //ConfigFactory.load().getString("quant4s.runMode")
    mode match {
      case TEST => H2Driver
      case DEV => MySQLDriver
      case PROD => PostgresDriver
      case _ => MySQLDriver
    }
  }
  val profile: JdbcProfile = getDriver

  import profile.simple._

  implicit val JavaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  /**
    *
    * @param ppdName
    * @param balance
    * @param uid
    * @param openId
    * @param accessToken
    * @param refreshToken
    * @param expiresIn
    * @param lastAccessDate
    * @param status 0:拍拍贷未授权用户 1: 拍拍贷授权用户打开自动投标 2: 拍拍贷授权用户取消自动投标
    */

  case class PpdUser(ppdName: String, var balance: Double, uid: Int, var investAmount: Int, var reserveAmount: Int,
                     var startRate: Double, var openId: String, var accessToken: String,
                     var refreshToken: String, var expiresIn: Int, var lastAccessDate: Date, var status: Int = 1)
  class PpdUsers(tag: Tag) extends Table[PpdUser](tag, "T_PPDUSER") {
    def ppdName = column[String]("PPD_NAME", O.PrimaryKey)
    def balance = column[Double]("PPD_BALANCE")
    def uid = column[Int]("USER_ID")
    def investAmount = column[Int]("INVEST_AMOUNT")
    def reservedAmount = column[Int]("RESERVED_AMOUNT")
    def startRate =  column[Double]("START_RATE")
    def openId  =column[String]("OPEN_ID")
    def accessToken = column[String]("ACCESS_TOKEN")
    def refreshToken = column[String]("REFRESH_TOKEN")
    def expiresIn = column[Int]("EXPIRES_IN")
    def lastAccessDate = column[Date]("LAST_ACCESS_DATE")
    def status = column[Int]("STATUS")

    override def * = (ppdName, balance, uid, investAmount, reservedAmount, startRate,
      openId, accessToken, refreshToken, expiresIn, lastAccessDate, status) <>(PpdUser.tupled, PpdUser.unapply)
  }
  val gPpdUsers = TableQuery[PpdUsers]

  case class User(id: Option[Int], username: String, password: String, grade: Int, var balance: Double, var lastBidTime: Option[Date], status: Int = 0)
  class Users(tag: Tag) extends Table[User](tag, "T_USER") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def username = column[String]("USERNAME")
    def password = column[String]("PASSWORD")
    def grade = column[Int]("GRADE")
    def balance = column[Double]("BALANCE")
    def lastBidTime = column[Date]("LAST_BID_TIME")
    def status = column[Int]("STATUS")

    override def * = (id.?, username, password, grade,balance, lastBidTime.?, status) <>(User.tupled, User.unapply)
  }
  val gUsers = TableQuery[Users]

  case class Strategy(id: Option[Int], name: String, ppdName: String, kind: Int, json: String)
  class Strategies(tag: Tag) extends Table[Strategy](tag, "T_STRATEGY") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def ppdUser = column[String]("PPD_USER")
    def kind = column[Int]("KIND")
    def json = column[String]("JSON_STR", O.DBType("varchar(5000)"))

    override def * = (id.?, name, ppdUser, kind, json) <>(Strategy.tupled, Strategy.unapply)
  }
  val gStrategies = TableQuery[Strategies]

  case class PpdUserStrategy(id: Option[Long], sid: Int, ppdUser: String, var status: Int, var amount: Int, var upLimit: Int, var start:Int)
  class PpdUserStrategies(tag: Tag) extends Table[PpdUserStrategy](tag, "T_PPDUSER_STRATEGY") {
    def id= column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def sid = column[Int]("STRATEGY_ID")
    def ppdUser = column[String]("PPD_NAME")
    def status = column[Int]("STATUS")
    def amount = column[Int]("AMOUNT")
    def upLimit = column[Int]("UP_LIMIT")
    def start = column[Int]("START")

    override def * = (id.?, sid, ppdUser, status, amount, upLimit, start) <>(PpdUserStrategy.tupled, PpdUserStrategy.unapply)
  }
  val gppdUserStartegies = TableQuery[PpdUserStrategies]

  case class BidLog(id: Option[Long], listingId: Long, ppdName: String, sid: Int, sname: String, amount: Int, date: Date = new Date(), simulate: Int =  0)
  class BidLogs(tag: Tag) extends Table[BidLog](tag, "T_BID_LOG") {
    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def listingId = column[Long]("LISTING_ID")
    def ppdName = column[String]("PPD_NAME")
    def sid = column[Int]("STRATEGY_ID")
    def sname = column[String]("STRATEGY_NAME")
    def amount = column[Int]("AMOUNT")
    def date = column[Date]("BID_DATE")
    def simulate = column[Int]("SIMULATE")

    override def * =(id.?, listingId, ppdName, sid, sname,amount, date, simulate) <> (BidLog.tupled, BidLog.unapply)
  }
  val gBidLogs = TableQuery[BidLogs]

  class BidLogSims(tag: Tag) extends Table[BidLog](tag, "T_BID_LOG_SIM") {
    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def listingId = column[Long]("LISTING_ID")
    def ppdName = column[String]("PPD_NAME")
    def sid = column[Int]("STRATEGY_ID")
    def sname = column[String]("STRATEGY_NAME")
    def amount = column[Int]("AMOUNT")
    def date = column[Date]("BID_DATE")
    def simulate = column[Int]("SIMULATE")

    override def * =(id.?, listingId, ppdName, sid, sname,amount, date, simulate) <> (BidLog.tupled, BidLog.unapply)
  }
  val gBidLogSims = TableQuery[BidLogSims]

  // =================== 借款信息 ======================
  case class LoanInfoJson(listingId: Long, title: String, status: Int, loanInfo: String, htmlInfo:String)
  class LoanInfoJsons(tag: Tag) extends Table[LoanInfoJson](tag, "T_LOAN_INFO_JSON") {
    def listingId = column[Long]("LISTING_ID")
    def title = column[String]("TITLE")
    def status = column[Int]("STATUS")
    def json = column[String]("LOAN_INFO", O.DBType("varchar(2000)"))
    def htmlInfo = column[String]("HTML_INFO", O.DBType("varchar(1000)"))

    override def * = (listingId, title, status, json, htmlInfo) <> (LoanInfoJson.tupled, LoanInfoJson.unapply)
  }
  val gloanInfoJsons = TableQuery[LoanInfoJsons]

  case class LoanInfoExt(listingId: Long, bi: BasicInfo, li: ListingInfo, ci: CreditInfo, ai: AuditInfo, si: StatInfo, pi: PageInfo, status: Int = 2)
  case class BasicInfo(borrowName: String, firstBidTime: String, lastBidTime: String, lenderCount: Int,
                       auditingTime: String, remainFunding: Int, deadLineTimeOrRemindTimeStr: String,
                       firstSuccessBorrowTime: String, registerTime: String)
  case class ListingInfo(months: Int, currentRate: Double, amount: Int, creditCode: String)
  case class CreditInfo(certificateValidate: Int, nciicIdentityCheck: Int, phoneValidate: Int,
                        videoValidate: Int, creditValidate: Int, educateValidate: Int)
  case class AuditInfo(age: Int, gender: Int, educationDegree: String, graduateSchool: String, studyStyle: String)
  case class StatInfo(successCount: Int, wasteCount: Int, cancelCount: Int, failedCount: Int,
                      normalCount: Int, overdueLessCount: Int, overdueMoreCount: Int, owingPrincipal: Int,
                      owingAmount: Int, amountToReceive: Int )
  case class PageInfo(sumLoanAmount: Double, maxLoanAmount:Double, highestDebt: Double)


  /**
    * 借款信息
    * @param tag
    */
  class LoanInfoes(tag: Tag) extends Table[LoanInfoExt](tag, "T_LOAN_INFO"){
    def ListingId = column[Long]("LISTING_ID", O.PrimaryKey)
    def status = column[Int]("STATUS")

    // BasicInfo
    def BorrowName = column[String]("BORROW_NAME")
    def FirstBidTime = column[String]("FIRST_BID_TIME", O.Nullable)
    def LastBidTime = column[String]("LAST_BID_TIME", O.Nullable)
    def LenderCount = column[Int]("LENDER_COUNT")
    def AuditingTime = column[String]("AUDITING_TIME", O.Nullable)
    def RemainFunding = column[Int]("REMAIN_FUNDING")
    def DeadLineTimeOrRemindTimeStr = column[String]("DEAD_LINE_TIME_OR_RTS", O.Nullable)
    def FirstSuccessBorrowTime = column[String]("FIRST_SUCC_BOR_TIME", O.Nullable)
    def RegisterTime = column[String]("REGISTER_TIME")

    // ListingInfo
    def Months = column[Int]("MONTHS")
    def CurrentRate = column[Double]("CURRENT_RATE")
    def Amount = column[Int]("AMOUNT")
    def CreditCode = column[String]("CREDIT_CODE")

    // CreditInfo
    def CertificateValidate = column[Int]("CERTIFICATE_VALIDATE")
    def NciicIdentityCheck = column[Int]("NCIIC_ID_CHECK")
    def PhoneValidate = column[Int]("PHONE_VALIDATE")
    def VideoValidate = column[Int]("VIDEO_VALIDATE")
    def CreditValidate = column[Int]("CREDIT_VALIDATE")
    def EducateValidate = column[Int]("EDUCATE_VALIDATE")

    // AuditInfo
    def Age = column[Int]("AGE")
    def Gender = column[Int]("GENDER")
    def EducationDegree = column[String]("EDUC_DEGREE", O.Nullable)
    def GraduateSchool = column[String]("GRADUATE_SCHOOL", O.Nullable)
    def StudyStyle = column[String]("STUDY_STYLE", O Nullable)

    // StatInfo
    def SuccessCount = column[Int]("SUCCESS_COUNT")
    def WasteCount = column[Int]("WASTE_COUNT")
    def CancelCount = column[Int]("CANCEL_COUNT")
    def FailedCount = column[Int]("FAILED_COUNT")
    def NormalCount = column[Int]("NORMAL_COUNT")
    def OverdueLessCount = column[Int]("OVERDUE_LESS_COUNT")
    def OverdueMoreCount = column[Int]("OVERDUE_MORE_COUNT")
    def OwingPrincipal = column[Int]("OWING_PRINCIPAL")
    def OwingAmount = column[Int]("OWING_AMOUNT")
    def AmountToReceive = column[Int]("AMOUNT_TO_REC")

    // Page Info
    def sumLoanAmount = column[Double]("SUM_LOAN_AMOUNT")
    def maxLoanAmount = column[Double]("MAX_LOAN_AMOUNT")
    def highestDebt = column[Double]("HIGHEST_DEBT")

    override def * = (ListingId,
      (BorrowName, FirstBidTime, LastBidTime, LenderCount, AuditingTime, RemainFunding,
        DeadLineTimeOrRemindTimeStr, FirstSuccessBorrowTime, RegisterTime),
      (Months, CurrentRate, Amount, CreditCode) ,
      (CertificateValidate, NciicIdentityCheck, PhoneValidate, VideoValidate, CreditValidate, EducateValidate),
      (Age, Gender, EducationDegree, GraduateSchool, StudyStyle),
      (SuccessCount, WasteCount, CancelCount, FailedCount, NormalCount, OverdueLessCount, OverdueMoreCount, OwingPrincipal,
        OwingAmount, AmountToReceive),
      (sumLoanAmount, maxLoanAmount, highestDebt),
      status
      ).shaped <> (
      { case (listingId, bi, li, ci, ai, si, pi, status) =>
        LoanInfoExt(listingId,
        BasicInfo.tupled.apply(bi),
        ListingInfo.tupled.apply(li),
        CreditInfo.tupled.apply(ci),
        AuditInfo.tupled.apply(ai),
        StatInfo.tupled.apply(si),
        PageInfo.tupled.apply(pi),
        status)},
        { li: LoanInfoExt =>
          def f1(p: BasicInfo) = BasicInfo.unapply(p).get
          def f2(p: ListingInfo) = ListingInfo.unapply(p).get
          def f3(p: CreditInfo) = CreditInfo.unapply(p).get
          def f4(p: AuditInfo) = AuditInfo.unapply(p).get
          def f5(p: StatInfo) = StatInfo.unapply(p).get
          def f6(p: PageInfo) = PageInfo.unapply(p).get
          Some((li.listingId, f1(li.bi), f2(li.li), f3(li.ci), f4(li.ai), f5(li.si), f6(li.pi), li.status))
        })
  }
  val gLoanInfos = TableQuery[LoanInfoes]

  /**
    * 投标信息
    * @param id
    * @param name
    * @param listingId
    * @param amount
    * @param bidTime
    */
  case class Bider(id: Option[Long], name: String, listingId: Int, amount: Int, bidTime: Date)
  class Biders(tag: Tag) extends Table[Bider](tag, "T_BIDER") {
    def id = column[Long]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def listingId = column[Int]("LISTING_ID")
    def amount = column[Int]("AMOUNT")
    def bidTime = column[Date]("BID_TIME")

    override def * : ProvenShape[Bider] = (id.?, name, listingId, amount, bidTime) <>(Bider.tupled, Bider.unapply)
  }
  val gBiders = TableQuery[Biders]


  case class Overdue(listingId: Long, maxDays: Int, count: Int)
  class Overdues(tag: Tag) extends Table[Overdue](tag, "T_OVERDUE") {
    def listingId = column[Long]("LISTING_ID", O.PrimaryKey)
    def maxDays = column[Int]("MAX_DAYS")
    def count = column[Int]("COUNT")

    override def * = (listingId, maxDays, count) <> (Overdue.tupled, Overdue.unapply)
  }
  val gOverdues = TableQuery[Overdues]
}
