package com.p2pgenius.user

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.p2pgenius.persistence.{PersistAction, PersistActionType, PersisterActor, PpdUser, User}
import com.p2pgenius.ppdService._
import com.p2pgenius.restful.UIUser

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
  implicit val timeout = Timeout(3 seconds)

  var userRefs = new mutable.HashMap[String, ActorRef]()
  val persisRef = context.actorSelection("/user/%s".format(PersisterActor.path))
  var ppdUsers = mutable.HashMap[String , PpdUser]()
  var users = mutable.HashMap[String, User]()

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("启动用户管理Actor")
    super.preStart()
    context.system.scheduler.scheduleOnce(2 seconds, self, "INIT")
  }

  override def receive: Receive = {
    case "INIT" =>  {
      fetchPpdUsers()
      fetchUsers()
    }

    case ServiceAction(ServiceActionType.AUTHORIZE_USER, au) =>authorizeUser(au.asInstanceOf[(String, String)])
    case ServiceAction(ServiceActionType.SIGN_IN, u) => signIn(u.asInstanceOf[UIUser])
    case ServiceAction(ServiceActionType.SIGN_UP, u) => createUser(u.asInstanceOf[UIUser])
    case ServiceAction(ServiceActionType.REMOVE_PPD_USER, user) =>removeUser(user.asInstanceOf[PpdUser])
    case ServiceAction(ServiceActionType.FETCH_MY_PPD_USER_LIST, username) => fetchMyRelativePpdUsers(username.asInstanceOf[String])

//    case FetchMyRelativePpdUsers(username) =>fetchMyRelativePpdUsers(username)
//    case u: UIUser => createUser(u)
//    case SignIn(u) => signIn(u)
//    case ServiceAction(ServiceActionType.SIGN_IN, u) => signIn(u.asInstanceOf[UIUser])
//    case au: AuthorizeUser => authorizeUser(au)
//    case remove: RemovePpdUser => removeUser(remove.ppdUser)
    case _ => log.debug("unsupported message")
  }

  def signIn(uIUser: UIUser): Unit = {
    val user = users.get(uIUser.username)
    if(user != None &&
      user.get.password == uIUser.password &&
      user.get.username == uIUser.username) {
      sender ! Result(0, "成功登录", content = user)
    } else {
      sender ! Result(1, "用户名或密码不正确")
    }
  }

  def createUser(user: UIUser): Unit = {
    // 检查本地缓存
    if(users.contains(user.username)) {
      sender ! Result(1, "用户已经存在")
    } else {
      val sender_old = sender
      val future = persisRef ? PersistAction(PersistActionType.INSERT_USER, User(None,user.username, user.password, 0, 0, None, 0))
      future onSuccess {
        case u: User  => {
          log.debug("成功保存用户")
          users += (u.username -> u)
          sender_old ! Result(0, "成功", u)
        }
      }

      future onFailure {
        case e: Exception =>
          log.error("保存用户失败")
          sender_old ! Result(1, "用户已经存在")
      }


    }
  }

  def fetchUsers(): Unit = {
    log.debug("请求用户数据")
    val future = persisRef ? PersistAction(PersistActionType.FETCH_ALL_USERS)
    future onSuccess {
      case us: List[User]  => {
        log.debug("接收到用户数据。 %d 个用户".format(us.size))
        for(u <- us) this.users += (u.username -> u)
      }
    }

    future onFailure {
      case e: Exception => log.error("接收到拍拍贷 用户数据失败")
    }
  }

  def  fetchPpdUsers(): Unit = {
    log.debug("请求拍拍贷用户数据")
    val future = persisRef ? PersistAction(PersistActionType.FETCH_ALL_PPD_USERS)
    future onSuccess {
      case users: List[PpdUser]  => {
        log.debug("接收到拍拍贷 用户数据。 %d 个用户".format(users.size))
        for(u <- users) createUser(u)
      }
    }

    future onFailure {
      case e: Exception => log.error("接收到拍拍贷 用户数据失败")
    }
  }

  def fetchMyRelativePpdUsers (username: String): Unit = {
    log.debug("获取%s的关联账户".format(username))
    val pu = users.get(username)
    if(pu == None)  {
      log.error("客户端传递过来未缓存的用户数据， 非法访问")
      sender ! Result(1, "非法用户")
    } else {
      val uid = pu.get.id.get
      var rpu = List[PpdUser]()
      ppdUsers.foreach(m => {
        if(m._2.uid == uid)  rpu = m._2 +: rpu
      })

      sender ! Result(0, "", rpu)
    }
  }

  def authorizeUser(au: (String, String)): Unit =  {
    val sender_old = sender
    val ref = context.actorOf(AuthorizeActor.props())
    val future = ref ? ServiceAction(ServiceActionType.AUTHORIZE_USER, au)
    future onSuccess {
      case user: PpdUser  => {
        if(ppdUsers.contains(user.ppdName))
          log.debug("authorize duplicate")
        else {
          log.debug("授权完成, 新增加一个授权用户" + user.toString )
          val tu = user.copy( uid = users.get(au._2).get.id.get)
          persisRef ! PersistAction(PersistActionType.INSERT_PPD_USER, tu)
          createUser(tu)
        }
        sender_old ! Result(0, "", user.ppdName)
      }
    }

    future onFailure {
      case e: Exception => log.debug("授权失败")
    }
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
    val ref = context.actorOf(PpdUserActor.props(user), user.ppdName)
    userRefs += (user.ppdName -> ref)
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

//case class AuthorizeUser(code: String, username: String)
//case class RemovePpdUser(ppdUser: PpdUser)
//case class FetchMyRelativePpdUsers(username: String)
//case class CreateUser(user: UIUser)
//case class SignIn(uIUser: UIUser)

