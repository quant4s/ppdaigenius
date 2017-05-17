package com.p2pgenius.user

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{PersisterActor, PpdUser}
import com.p2pgenius.ppdService._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * 管理所有的用户
  * 1. 从数据库中读取用户数据
  * 2. 一个用户取消授权
  *
  * 1. 根据code 增加一个授权用户
  * 2. 获取用户名
  * 3.
  */
class UserManagerActor extends Actor with ActorLogging {
  var userRefs = new mutable.HashMap[String, ActorRef]()
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  var ppdUsers = mutable.HashMap[String , PpdUser]()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("启动用户管理Actor")
    super.preStart()
    context.system.scheduler.scheduleOnce(2 seconds, self, "FETCH_ALL_PPD_USERS")
  }

  override def receive: Receive = {
    case "FETCH_ALL_PPD_USERS" => fetchPpdUsers()
    case FetchMyRelativePpdUsers(ppdName) =>fetchMyRelativePpdUsers(ppdName)

    case au: AuthorizeUser => authorizeUser(au)
    case remove: RemovePpdUser => removeUser(remove.ppdUser)
    case _ =>
  }

  def  fetchPpdUsers(): Unit = {
    log.debug("请求拍拍贷用户数据")
    val sender1 = sender
    val ref = context.actorOf(AuthorizeActor.props())
    implicit val timeout = Timeout(5 seconds)
    val future = persisRef ? "FETCH_ALL_PPD_USERS"
    future onSuccess {
      case users: List[PpdUser]  => {
        log.debug("接收到拍拍贷 用户数据。 %d 个用户".format(users.size))
        for(u <- users) {
           createUser(u)
        }
      }
    }

    future onFailure {
      case e: Exception => log.error("接收到拍拍贷 用户数据失败")
    }
  }

  def fetchMyRelativePpdUsers (ppdName: String): Unit = {
    log.debug("获取%s的关联账户".format(ppdName))
    val pu = ppdUsers.get(ppdName)
    if(pu == None) log.error("客户端传递过来未缓存的用户数据， 非法访问")
    else {
      val uid = pu.get.uid
      var rpu = List[PpdUser]()
      ppdUsers.foreach(m => {
        if(m._2.uid == uid)  rpu = m._2 +: rpu
      })

      sender ! rpu
    }
  }

  def authorizeUser(au: AuthorizeUser): Unit =  {
    val sender_old = sender
    val ref = context.actorOf(AuthorizeActor.props())
    implicit val timeout = Timeout(5 seconds)
    val future = ref ? au
    future onSuccess {
      case user: PpdUser  => {
        log.debug("授权完成, 新增加一个授权用户" + user.toString )
        persisRef ! user
        createUser(user)
        sender_old ! user.ppdName
      }
    }

    future onFailure {
      case e: Exception => log.debug("授权失败")
    }

    log.debug("future 后面的代码" )


  }

  def removeUser(ppdUser: PpdUser): Unit = {
    userRefs.get(ppdUser.ppdName).get ! PoisonPill
    persisRef ! ppdUser.copy(status = 0)
  }


  /**
    * 创建用户Actor
    * @param user
    */
  def createUser(user: PpdUser): Unit = {
    ppdUsers += (user.ppdName -> user)
    if(user.status != 0) {
      val ref = context.actorOf(PpdUserActor.props(user), user.ppdName)
      userRefs += (user.ppdName -> ref)
    }
  }

  /**
    * 获取排序后的用户列表
    */
  def getSortedUserList(): Unit = {

  }

}

object UserManagerActor {
  val path = "user_manager"

  def props(): Props = {
    Props(classOf[UserManagerActor])
  }
}

case class AuthorizeUser(code: String)
case class RemovePpdUser(ppdUser: PpdUser)
case class FetchMyRelativePpdUsers(ppdName: String)

