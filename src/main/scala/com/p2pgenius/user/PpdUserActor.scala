package com.p2pgenius.user

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{BidLog, PersistAction, PersistActionType, PersisterActor, PpdUser, PpdUserStrategy, Strategy, User}
import com.p2pgenius.ppdService._
import com.p2pgenius.strategies._
import com.ppdai.open.AuthInfo
import org.json4s.jackson.JsonMethods.parse

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * 用户Actor管理自己的Actor
  * 1. 投标（转移到策略Actor 中做） ok
  * 2. 查询余额     ok
  * 3. 查询订阅的策略
  * 4. 订阅一个策略
  * 5. 取消订阅策略
  * 6. 停止自动投标   ok
  */
class PpdUserActor(ppdUser: PpdUser) extends Actor with PpdRemoteService with ActorLogging {
  val user: User = null.asInstanceOf[User]
  val bidRef = context.actorSelection("/user/%s".format(BidActor.path))
  val spref = context.actorSelection("/user/%s".format(StrategyPoolActor.path))
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  override val connection: HttpURLConnection = createUrlConnection(QUERY_BALANCE_URL)

  var myStrategies = HashMap[Int, PpdUserStrategy]()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    log.debug("新建了一个用户Actor: %s".format(ppdUser.ppdName))

    // 定时作业 30min 一次 获取余额, 3秒之后，获取我的策略
    context.system.scheduler.schedule(2 seconds, 30 minutes, self, "QUERY_BALANCE")
    context.system.scheduler.scheduleOnce(30 seconds, self, "INIT")
  }

  override def receive: Receive = {
    case "INIT" => init()
    case "QUERY_BALANCE" => queryBalance() // 查询余额
    case "REFRESH_TOKEN" => refreshToken() // 刷新Token

    case ServiceAction(ServiceActionType.SUB_STRATEGY, s: (String, Int)) => subscribeStrategy(s._2)
    case ServiceAction(ServiceActionType.UNSUB_STRATEGY, s: (String, Int)) => unSubscribeStrategy(s._2)
    case ServiceAction(ServiceActionType.FETCH_MY_STRATEGY_lIST_SETTING, _) =>fetchMyStrategies()
    case ServiceAction(ServiceActionType.FETCH_MY_PPD_USER_LIST, _) => fetchRelativeUsers()
//    case li: LoanInfoWrapper => // receiveLoan(li.strategy, li.loanInfo.ListingId)  // 投标
//    case ll: LoanListWrapper => // receiveLoan(ll.strategy, ll.loanList.ListingId) // 投标
//    case b: BidResultWrapper =>  receiveBid(b) // 接收到投标结果

    case x: Any => log.warning("不支持的消息%s".format(x.toString))// 更新策略下单金额
  }

  /**
    * 从数据库中读取用户关注的策略数据，并订阅，
    */
  def init(): Unit = {
    log.debug("[PpdUserActor.init]初始化: 读取用户%s订阅的策略".format(ppdUser.ppdName))
    val future = persisRef ? PersistAction(PersistActionType.FETCH_USER_SUB_STRATEGIES, ppdUser.ppdName)     // FetchMyStrategies(ppdUser.ppdName)
    future onSuccess {
      case strategies: List[PpdUserStrategy]  => {
        log.debug("构建订阅的策略缓存%d".format(strategies.size))
        for(ff <- strategies) {
          myStrategies += (ff.sid -> ff)
        }


        myStrategies.foreach(s => {
          if(s._2.status == 1) {
            log.debug("用户%s请求订阅策略%d的策略".format(ppdUser.ppdName,s._2.status))
            self ! SubStrategy(ppdUser.ppdName, s._2.sid)
          }
          else
            self ! UnsubStrategy(ppdUser.ppdName, s._2.sid)
        })
      }
    }
    future onFailure {
      case e: Exception => log.error("读取订阅策略数据失败%s".format(e.getMessage))
    }

  }

  def fetchMyStrategies() = {
    log.debug("获取用户订阅的策略")
    sender ! myStrategies.values.toList
  }

   def  getBids(page: Int, size: Int, date: Date): Unit ={

   }

  /**
    * 订阅策略产生的借款标的
    * @param id 策略ID
    */
  def subscribeStrategy(id: Int): Unit = {
    if(myStrategies.get(id)== None) {
      myStrategies += (id -> PpdUserStrategy(None, id, ppdUser.ppdName, 1, 58, 100000, 0))
    }

    myStrategies.get(id).get.status = 1
    spref ! ServiceAction(ServiceActionType.SUB_STRATEGY, (ppdUser, id))

    log.debug("%s订阅%d到数据库".format(ppdUser.ppdName, id))
    persisRef ! PersistAction(PersistActionType.INSERT_OR_UPDATE_SUB_OR_UNSUB_STRATEGY, myStrategies.get(id).get )
  }

  /**
    * 取消订阅策略产生的借款标的
    * @param id
    */
  def unSubscribeStrategy(id: Int): Unit = {
    if(myStrategies.get(id)== None) {
      myStrategies += (id -> PpdUserStrategy(None, id, ppdUser.ppdName, 0, 58, 100000, 0))
    }

    myStrategies.get(id).get.status = 0
    spref ! ServiceAction(ServiceActionType.UNSUB_STRATEGY, (ppdUser, id))

    log.debug("%scancel 订阅%d 到数据库".format(ppdUser.ppdName, id))
    persisRef ! PersistAction(PersistActionType.INSERT_OR_UPDATE_SUB_OR_UNSUB_STRATEGY, myStrategies.get(id).get )
  }

  /**
    * 接收到借款标的
    * @param strategy 策略编号
    * @param listingId 列表编号
    */
  def receiveLoan(strategy: Strategy, listingId: Long): Unit = {
    // 检测有没有足够的资金投， 如果有就投标
    val amount = myStrategies.get(strategy.id.getOrElse(0)).get.amount
    if(ppdUser.balance > amount) bidRef ! new Bid(strategy, ppdUser, listingId, amount)
  }

  /**
    * 接收到投标结果信息
    * @param bid
    */
  def receiveBid(bid: BidResultWrapper): Unit = {
    // TODO 保存到投标异常记录表
    log.debug("listId： %d, 返回代码：%d".format(bid.bidResult.ListingId, bid.bidResult.Result))
    bid.bidResult.Result match {
      case 1002 =>  // 用户取消了授权
      case 0 =>
        ppdUser.balance -= bid.bidResult.ParticipationAmount
        persisRef ! BidLog(None, bid.bidResult.ListingId, bid.ppdUser.ppdName, bid.strategy.id.getOrElse(0), bid.strategy.name, bid.bidResult.ParticipationAmount)
      case _ =>
    }
  }

//  /**
//    * 改变自动投标状态
//    * @param auto
//    */
//  def changeAutoBid(auto: Boolean): Unit = {
//    log.debug("改变自动投标状态为%s".format(auto.toString))
//    ppdUser.status = if(auto) 1 else 2
//    persisRef ! ppdUser
//  }

  /**
    * 用户现金余额
    */
  def queryBalance(): Unit = {
    log.debug("自动查询用户现金余额")
    val queryBalanceResult = send(createUrlConnection(QUERY_BALANCE_URL),"")(ppdUser.accessToken)
    if(queryBalanceResult == null)
      log.debug("[queryBalance] %s a error raised".format(ppdUser.ppdName))
    else {
      val queryBalance = if (queryBalanceResult.sucess) {
        val jv = parse(queryBalanceResult.context)
        jv.extract[QueryBalanceResult]
      } else
        null.asInstanceOf[QueryBalanceResult]
      ppdUser.balance = queryBalance.Balance.find(b => b.AccountCategory == "用户备付金.用户现金余额").get.Balance
    }
  }

  def refreshToken(): Unit = {
    val refreshTokenConn = createTokenConnection(REFRESH_TOKEN_URL)
    val dataOutputStream = new DataOutputStream(refreshTokenConn.getOutputStream())

    /******************** 获取授权参数 AppID code *********************/
    dataOutputStream.writeBytes("{\"AppID\":\"%s\",\"OpenID\":\"%s\",\"RefreshToken\":\"%s\"}".format(APP_ID, ppdUser.openId, ppdUser.refreshToken))
    dataOutputStream.flush()
    val inputStream = refreshTokenConn.getInputStream();
    val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
    val bufferedReader = new BufferedReader(inputStreamReader)
    val strResponse = bufferedReader.readLine()

    val authInfo = parse(strResponse).extract[AuthInfo]
    ppdUser.accessToken = authInfo.AccessToken
    ppdUser.refreshToken = authInfo.RefreshToken
    ppdUser.openId = authInfo.OpenID
    ppdUser.expiresIn = authInfo.ExpiresIn.toInt
  }

  /**
    * 获取关联用户
    */
  def fetchRelativeUsers(): Unit = {

  }

  def createTokenConnection(url: String) = {
    val serviceUrl = new URL(url)
    val urlConnection = serviceUrl.openConnection().asInstanceOf[HttpURLConnection]
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(true)
    urlConnection.setUseCaches(false)

    urlConnection.setRequestMethod("POST")
    urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
    urlConnection
  }
}

object PpdUserActor {
  def props(user: PpdUser): Props = {
    Props(classOf[PpdUserActor], user)
  }
}


case class SubStrategy(ppdName: String, sid: Int)
case class UnsubStrategy(ppdName: String, sid: Int)
