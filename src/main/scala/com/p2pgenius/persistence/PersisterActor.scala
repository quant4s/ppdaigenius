package com.p2pgenius.persistence

import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.p2pgenius.persistence.PersistActionType.PersistActionType
import com.p2pgenius.ppdService.{HtmlInfo, LoanInfo}

import org.json4s._
import org.json4s.jackson.JsonMethods.{compact, render}


/**
  *
  */
class PersisterActor extends Actor with ActorLogging {
  import profile.simple._

  var db = getDatabase
  implicit val session = db.createSession()

  implicit val JavaUtilDateMapper =
    MappedColumnType .base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))
  //installDb()

  def getDatabase(): Database = {
//    val TEST = "test"
//    val DEV = "dev"
//    val PROD = "prod"
//    val runMode = ConfigFactory.load().getString("ppdai.runMode")
    Database.forConfig("dev")
//    Database.forURL(url="jdbc:mysql://localhost:3306/ppdai?user=root&useUnicode=true&characterEncoding=UTF8", driver="com.mysql.jdbc.Driver")
  }

  override def receive: Receive = {
    // 初始化
    case "INIT" => installDb()
    // 保存信息
    // case li: LoanInfo => saveLoanInfo(li) // 保存借款信息或者更新状态
//    case b: Bider => saveBider(b)       // 保存借款的投标人
//    case bl: BidLog => saveBidLog(bl)   // 保存我的投标日志
//    case s: PpdUserStrategy => savePpdUserStrategy(s)
//    case pu: PpdUser => savePpdUser(pu)  // 保存或者更新PPD USER
//    case u: User => saveUser(u)    // 保存或者更新用户

    case PersistAction(pat, li) if(pat == PersistActionType.INSERT_LOAN) => saveLoanInfo(li.asInstanceOf[(String, Int, LoanInfo, HtmlInfo)])
    case PersistAction(pat, bider) if(pat == PersistActionType.INSERT_BIDDER) => saveBider(bider.asInstanceOf[Bider])
    case PersistAction(PersistActionType.INSERT_BID, bid)  => saveBidLog(bid.asInstanceOf[BidLog])

    case PersistAction(PersistActionType.INSERT_USER, user) => sender ! saveUser(user.asInstanceOf[User])
    case PersistAction(pat, pu) if(pat == PersistActionType.INSERT_PPD_USER) => savePpdUser(pu.asInstanceOf[PpdUser])
    case PersistAction(pat, s) if(pat == PersistActionType.INSERT_OR_UPDATE_STRATEGY) =>
      saveStrategy(s.asInstanceOf[Strategy])
    case PersistAction(pat, pu) if(pat == PersistActionType.INSERT_OR_UPDATE_SUB_OR_UNSUB_STRATEGY) =>
      savePpdUserStrategy(pu.asInstanceOf[PpdUserStrategy])
    case PersistAction(PersistActionType.INSERT_OR_UPDATE_SUB_OR_UNSUB_STRATEGY, uf) =>
      saveUser(uf.asInstanceOf[User])

    // 读取信息
//    case "FETCH_ALL_PPD_USERS" => fetchPpdUsers()   // 读取所有的拍拍贷用户信息
//    case "FETCH_ALL_STRATEGIES" => fetchStrategies() // 读取所有的策略
//    case FetchMyStrategies(name) =>fetchMyStrategies(name)  // 读取我的策略

    case PersistAction(PersistActionType.INSERT_OR_UPDATE_OVERDUE, overdue) => saveOverdue(overdue.asInstanceOf[Overdue])
    case PersistAction(pat, _) if(pat == PersistActionType.FETCH_ALL_PPD_USERS) =>fetchPpdUsers()
    case PersistAction(pat, _) if(pat == PersistActionType.FETCH_ALL_STRATEGIES) => fetchStrategies()
    case PersistAction(pat, name) if(pat == PersistActionType.FETCH_USER_SUB_STRATEGIES) =>fetchMyStrategies(name.asInstanceOf[String])
    case PersistAction(PersistActionType.FETCH_ALL_USERS, _) => fetchUsers()
    case PersistAction(PersistActionType.FETCH_MY_BIDS, req) => fetchMyBids(req.asInstanceOf[(String, Int, Int)])
//    case Fet"FETCH_ALL_PPD_USERS"chPpdUsers => fetchPpdUsers()   // 读取所有的拍拍贷用户信息
//    case FetchStrategies => fetchStrategies() // 读取所有的策略
//    case GetUser(ppdUser) => getUser(ppdUser)
//    case FetchMyRelativePpdUsers(ppdUser) => fetchMyRelativePpdUsers(ppdUser)
    case m:Any => log.warning("【PersisterActor】不支持的消息%s".format(m.toString))    // 读取所有Ppd用户
  }

  def installDb(): Unit = {
    val ddl = gUsers.ddl ++ gPpdUsers.ddl ++ gBiders.ddl ++ gBidLogs.ddl ++ gStrategies.ddl ++ gLoanInfos.ddl ++ gloanInfoJsons.ddl ++ gppdUserStartegies.ddl
    // ddl.drop
    ddl.create

    _initDatabase()
  }

  private def _initDatabase(): Unit = {
    // 用户
    saveUser(User(None, "testuser", "password", 5, 2000, None, 1))
    // 拍拍贷用户
    savePpdUser(PpdUser("pdu8623642206", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
//    savePpdUser(PpdUser("test1", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
//    savePpdUser(PpdUser("test2", 10000, 1, 59, 0, 18, "openid", "accesstoken", "refreshToken", 7500, new Date()))
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

  def saveOverdue(overdue: Overdue): Unit = {
//    implicit val formats: Formats = DefaultFormats
//    val json = compact(render(Extraction.decompose(overdue)))
    gOverdues.insertOrUpdate(overdue)
  }

  def saveLoanInfo(lia: (String, Int, LoanInfo, HtmlInfo)): Unit = {
    log.debug("保存借款信息")
    implicit val formats: Formats = DefaultFormats
    val json = compact(render(Extraction.decompose(lia._3)))
    val htmlInfo = compact(render(Extraction.decompose(lia._4)))
    val info:LoanInfoJson = LoanInfoJson(lia._3.ListingId, lia._1, lia._2, json, htmlInfo)
    gloanInfoJsons.insertOrUpdate(info)
    log.debug("保存借款信息")
    // 先检测数据库中是否存在数据
//    val li = lia._2
//    val infoExt: LoanInfoExt = LoanInfoExt(li.ListingId,
//      BasicInfo(li.BorrowName, li.FistBidTime, li.LastBidTime, li.LenderCount, li.AuditingTime, li.RemainFunding,
//        li.DeadLineTimeOrRemindTimeStr, li.FirstSuccessBorrowTime, li.RegisterTime),
//      ListingInfo(li.Months, li.CurrentRate, li.Amount, li.CreditCode),
//      CreditInfo(li.CertificateValidate, li.NciicIdentityCheck, li.PhoneValidate, li.VideoValidate,
//        li.CreditValidate, li.EducateValidate),
//      AuditInfo(li.Age, li.Gender, li.EducationDegree, li.GraduateSchool, li.StudyStyle),
//      StatInfo(li.SuccessCount, li.WasteCount, li.CancelCount, li.FailedCount, li.NormalCount, li.OverdueLessCount,
//        li.OverdueMoreCount, li.OwingPrincipal, li.OwingAmount, li.AmountToReceive),
//      PageInfo(li.TotalPrincipal, li.HighestPrincipal, li.HighestDebt))
//     gLoanInfos.insertOrUpdate(infoExt)
  }

  def saveBider(bider: Bider): Unit = {
    gBiders.map( b => b) += bider
  }

  def saveBidLog(bidLog: BidLog): Unit = {
    log.debug("保存投标日志%s".format(bidLog.toString))
    if(bidLog.simulate == 1) {
      // simulate
      gBidLogSims.map(b => b) += bidLog
    } else {
      gBidLogs.map(b => b) += bidLog
    }
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
      query.map(p => ( p.password, p.grade, p.balance, p.lastBidTime, p.status))
        .update(user.password, user.grade,user.balance, user.lastBidTime.get, user.status)
      user
    }
  }

  def savePpdUser(ppdUser: PpdUser): Unit = {
    val q = gPpdUsers.filter(_.ppdName === ppdUser.ppdName)
    if(q.firstOption == None) {
//      val user = saveUser(User(None, 0, None))   // 保存为普通用户
      log.debug("[savePpdUser]create new ppd user %s".format(ppdUser.ppdName))
//      gPpdUsers.map( u => u ) += ppdUser.copy(uid = user.id.get)
      gPpdUsers.map( u => u ) += ppdUser.copy(uid = 1) // todo
    } else { // 附加授权, 改变自动投标状态 ，
      log.debug("[savePpdUser]update user %s".format(ppdUser.ppdName))
      val query = gPpdUsers.filter( pu => pu.ppdName === ppdUser.ppdName)
      gPpdUsers.map( u => u ) += ppdUser
    }
  }

  /**
    *
    * @param ppdUser
    */
  def updatePpdUserSetting(ppdUser: PpdUser) {
    val query = gPpdUsers.filter( pu => pu.ppdName === ppdUser.ppdName)
    query.map( pu => (pu.balance, pu.investAmount, pu.reservedAmount, pu.status))
    .update((ppdUser.balance, ppdUser.investAmount, ppdUser.reserveAmount, ppdUser.status))
  }

  /**
    *
    * @param ppdUser
    */
  def updatePpdUserAuthInfo(ppdUser: PpdUser): Unit = {
    val query = gPpdUsers.filter( pu => pu.ppdName === ppdUser.ppdName)
    query.map( pu => (pu.openId, pu.accessToken, pu.refreshToken, pu.expiresIn))
      .update((ppdUser.openId, ppdUser.accessToken, ppdUser.refreshToken, ppdUser.expiresIn))

  }

  def saveStrategy(strategy: Strategy): Unit = {
    log.debug("[PersisterActor.saveStrategy]")
    if(strategy.id == None) {
      val id = (gStrategies returning gStrategies.map(_.id) += strategy)
      sender ! strategy.copy(id = Some(id))
    } else {
      val q = gStrategies.filter(_.id === strategy.id.get)
      q.map( s => s).update(strategy)
      sender ! strategy
    }
  }

  /**
    * 获取所有的ppd用户数据
    */
  def fetchPpdUsers(): Unit = {
    log.debug("[PersisterActor.fetchPpdUsers]列出所有的用户")
    sender ! gPpdUsers.list
  }

  def fetchUsers(): Unit = {
    log.debug("[PersisterActor.fetchUsers]列出所有的用户")
    sender ! gUsers.list
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

//  /**
//    * 获取PPD USER 相应的User
//    * @param ppdUser
//    */
//  def getUser(ppdUser: PpdUser) = sender ! gUsers.filter(_.id === ppdUser.uid).take(1).firstOption.get

  def fetchMyBids(req: (String, Int, Int)) =  {
    val offset = req._2 * req._3
    sender ! gBidLogs.filter(b => b.ppdName === req._1).sortBy(b => b.id.desc).drop(offset).take(req._3)
//    sender ! gBidLogs.filter(_.ppdName === ppdName)
  }
//  def fetchMyRelativePpdUsers(ppdUser: PpdUser) = sender ! gPpdUsers.filter(_.uid === ppdUser.uid)
}

object PersisterActor {
  def props() = {
    Props(classOf[PersisterActor])
  }

  val path = "persister"
}

//case class FetchPpdUsers()
//case class GetUser(ppdUser: PpdUser)

case class PersistAction(action: PersistActionType, body: Any = null)

object PersistActionType extends Enumeration {
  type PersistActionType = Value
  val INSERT_LOAN,
      INSERT_BID,
      INSERT_BIDDER,

      INSERT_PPD_USER,
      INSERT_USER,
      UPDATE_PPD_USER_SETTING,
      UPDATE_PPD_USER_AUTHINFO,
      UPDATE_USER_FLOW,
      INSERT_OR_UPDATE_SUB_OR_UNSUB_STRATEGY,
      INSERT_OR_UPDATE_STRATEGY,

      FETCH_ALL_PPD_USERS,
      FETCH_ALL_USERS,
      FETCH_ALL_STRATEGIES,
      FETCH_USER_SUB_STRATEGIES,
      FETCH_MY_BIDS,

      INSERT_OR_UPDATE_OVERDUE
    = Value
}