package com.p2pgenius.persistence

import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.p2pgenius.ppdService.LoanInfo
import com.p2pgenius.strategies.FetchMyStrategies

/**
  *
  */
class PersisterActor extends Actor with ActorLogging {
  import profile.simple._

  var db = getDatabase
  implicit val session = db.createSession()

   installDb()

  def getDatabase(): Database = {
    val TEST = "test"
    val DEV = "dev"
    val PROD = "prod"
//    val runMode = ConfigFactory.load().getString("ppdai.runMode")
//    Database.forConfig("dev")
    Database.forURL(url="jdbc:mysql://192.168.174.101:3306/ppdai?user=root&password=1&useUnicode=true&characterEncoding=UTF8", driver="com.mysql.jdbc.Driver")
  }

  override def receive: Receive = {
    // 初始化
    case "INIT" => installDb()
    // 保存信息
    case li: LoanInfo => saveLoanInfo(li) // 保存借款信息或者更新状态
    case b: Bider => saveBider(b)       // 保存借款的投标人
    case bl: BidLog => saveBidLog(bl)   // 保存我的投标日志
    case s: PpdUserStrategy => savePpdUserStrategy(s)  //
    case pu: PpdUser => savePpdUser(pu)  // 保存或者更新PPD USER
    case u: User => saveUser(u)    // 保存或者更新用户

    // 读取信息
    case "FETCH_ALL_PPD_USERS" => fetchPpdUsers()   // 读取所有的拍拍贷用户信息
    case "FETCH_ALL_STRATEGIES" => fetchStrategies() // 读取所有的策略
//    case Fet"FETCH_ALL_PPD_USERS"chPpdUsers => fetchPpdUsers()   // 读取所有的拍拍贷用户信息
//    case FetchStrategies => fetchStrategies() // 读取所有的策略
    case FetchMyStrategies(name) =>fetchMyStrategies(name)  // 读取我的策略
    case GetUser(ppdUser) => getUser(ppdUser)
//    case FetchMyRelativePpdUsers(ppdUser) => fetchMyRelativePpdUsers(ppdUser)
    case _ =>     // 读取所有Ppd用户
  }

  def installDb(): Unit = {
    val ddl = gUsers.ddl ++ gPpdUsers.ddl ++ gBiders.ddl ++ gBidLogs.ddl ++ gStrategies.ddl ++ gLoanInfos.ddl ++ gppdUserStartegies.ddl
    ddl.drop
    ddl.create

    _initDatabase()
  }

  private def _initDatabase(): Unit = {
    // 用户
    saveUser(User(None, 2000, None, 1))
    // 拍拍贷用户
    savePpdUser(PpdUser("2", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
    savePpdUser(PpdUser("test1", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
    savePpdUser(PpdUser("test2", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
    // 策略
    saveStrategy(Strategy(None, "策略1", "", 1, ""))
    saveStrategy(Strategy(None, "策略2", "", 2, ""))
    saveStrategy(Strategy(None, "策略3", "2", 3, ""))
    saveStrategy(Strategy(None, "策略4", "", 4, ""))
    // 用户订阅策略
    savePpdUserStrategy(PpdUserStrategy(None, 1, "2", 1, 58, 1000, 0))
    savePpdUserStrategy(PpdUserStrategy(None, 2, "2", 0, 58, 1000, 0))
    savePpdUserStrategy(PpdUserStrategy(None, 3, "2", 1, 58, 1000, 0))
    savePpdUserStrategy(PpdUserStrategy(None, 4, "2", 0, 58, 1000, 0))
  }

  def saveLoanInfo(li: LoanInfo): Unit = {
    log.debug("保存借款信息")
    // 先检测数据库中是否存在数据
    val infoExt: LoanInfoExt = LoanInfoExt(li.ListingId,
      BasicInfo(li.BorrowName, li.FistBidTime, li.LastBidTime, li.LenderCount, li.AuditingTime, li.RemainFunding,
        li.DeadLineTimeOrRemindTimeStr, li.FirstSuccessBorrowTime, li.RegisterTime),
      ListingInfo(li.Months, li.CurrentRate, li.Amount, li.CreditCode),
      CreditInfo(li.CertificateValidate, li.NciicIdentityCheck, li.PhoneValidate, li.VideoValidate,
        li.CreditValidate, li.EducateValidate),
      AuditInfo(li.Age, li.Gender, li.EducationDegree, li.GraduateSchool, li.StudyStyle),
      StatInfo(li.SuccessCount, li.WasteCount, li.CancelCount, li.FailedCount, li.NormalCount, li.OverdueLessCount,
        li.OverdueMoreCount, li.OwingPrincipal, li.OwingAmount, li.AmountToReceive),
      PageInfo(li.sumLoanAmount, li.maxLoanAmount, li.highestDebt))
     gLoanInfos.insertOrUpdate(infoExt)
//    val query = gLoanInfos.filter(_.ListingId === li.ListingId)
//    if(true) {
//      val infoExt: LoanInfoExt = LoanInfoExt(li.ListingId,
//        BasicInfo(li.BorrowName, li.FistBidTime, li.LastBidTime, li.LenderCount, li.AuditingTime, li.RemainFunding,
//          li.DeadLineTimeOrRemindTimeStr, li.FirstSuccessBorrowTime, li.RegisterTime),
//        ListingInfo(li.Months, li.CurrentRate, li.Amount, li.CreditCode),
//        CreditInfo(li.CertificateValidate, li.NciicIdentityCheck, li.PhoneValidate, li.VideoValidate,
//          li.CreditValidate, li.EducateValidate),
//        AuditInfo(li.Age, li.Gender, li.EducationDegree, li.GraduateSchool, li.StudyStyle),
//        StatInfo(li.SuccessCount, li.WasteCount, li.CancelCount, li.FailedCount, li.NormalCount, li.OverdueLessCount,
//          li.OverdueMoreCount, li.OwingPrincipal, li.OwingAmount, li.AmountToReceive))
//
//      gLoanInfos.map(s => s) += infoExt
//    } else {
//      query.map( p => p.status).update( li.status)
//    }
  }

  def saveBider(bider: Bider): Unit = {
    gBiders.map( b => b) += bider
  }

  def saveBidLog(bidLog: BidLog): Unit = {
    gBidLogs.map( b => b) += bidLog
  }

  def savePpdUserStrategy(s: PpdUserStrategy): Unit = {
    log.debug("保存用户%s策略%d关系".format(s.ppdUser, s.sid))
    // 检测是否存在，更新或者创建
    if(s.id == None)
      gppdUserStartegies.map(b => b) += s
    else {
      val ppduserQuery = gppdUserStartegies.filter(_.id === s.id.get)
      ppduserQuery.map(e =>(e.status, e.amount, e.upLimit, e.start)).update(s.status, s.amount, s.upLimit, s.start)
    }
  }

  def saveUser(user: User): User = {
    // 首先检测是否存在用户， 不存在插入， 存在就更新
    if(user.id == None) {
      val id = (gUsers returning gUsers.map(_.id) += user)
      user.copy(id = Some(id))
    } else {
      // 更新金额， 状态， 投标时间
      val query = gUsers.filter(_.id === user.id.get)
      query.map(p => (p.balance, p.lastBidTime, p.status)).update(user.balance, user.lastBidTime.get, user.status)
      user
    }
  }

  def savePpdUser(ppdUser: PpdUser): Unit = {
    if(ppdUser.uid == 0) {     // 新用户 TODO: 检查PPDNAME 是否存在
      val user = saveUser(User(None, 0, None))   // 保存为普通用户
      gPpdUsers.map( u => u ) += ppdUser.copy(uid = user.id.get)
    } else { // 附加授权, 改变自动投标状态，最后一次投标时间等等
      gPpdUsers.map( u => u ) += ppdUser
    }
  }

  def saveStrategy(strategy: Strategy): Unit = {
    if(strategy.id == 0) {

    } else {
      gStrategies.map( s => s) += strategy
    }
  }

  /**
    * 获取所有的ppd用户数据
    */
  def fetchPpdUsers(): Unit = {
    log.debug("列出所有的用户")
    sender ! gPpdUsers.list
  }

  /**
    * 读取用户所有订阅的策略
    * @param name
    */
  def fetchMyStrategies(name: String): Unit = {
    log.debug("获取用户%s所有的策略".format(name))
    sender ! gppdUserStartegies.filter(_.ppdUser === name).list
  }

  /**
    * 读取所有的策略
    */
  def fetchStrategies(): Unit = {
    log.debug("读取所有的策略")
    sender ! gStrategies.list
  }

  /**
    * 获取PPD USER 相应的User
    * @param ppdUser
    */
  def getUser(ppdUser: PpdUser) = sender ! gUsers.filter(_.id === ppdUser.uid).take(1).firstOption.get

//  def fetchMyRelativePpdUsers(ppdUser: PpdUser) = sender ! gPpdUsers.filter(_.uid === ppdUser.uid)
}

object PersisterActor {
  def props() = {
    Props(classOf[PersisterActor])
  }

  val path = "persister"
}

//case class FetchPpdUsers()
case class GetUser(ppdUser: PpdUser)
