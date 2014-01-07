
package test.repository

import models.repository.UserRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class MemoryUserRepository extends UserRepository {

  val time = 1000 * 60 * 10

  var users: Map[String, Map[String, String]] =
    Map("test2@example.com" -> Map("password" -> "password", "token" -> "token"))

  override def create(email: String, password: String, token: String) = {
    if (users.contains(email))
      Future.failed(new Exception())
    else
      Future.successful(users += email -> Map("password" -> password, "token" -> token))

  }

  override def getToken(email: String) =
    Future.successful(users.get(email).flatMap(_.get("token")))

  override def exists(email: String) =
    Future.successful(users.contains(email))

  override def getPasswordHash(email: String) =
    Future.successful(users.get(email).flatMap(_.get("password")))

  override def changePassword(email: String, newPassword: String) =
    Future.successful(updateFieldOf(email)("password" -> newPassword))

  override def setResetPassword(email: String, token: String) = Future.successful {
    updateFieldOf(email)("resetToken" -> token)
    updateFieldOf(email)("resetTime" -> (System.currentTimeMillis + time).toString)
  }

  override def resetPassword(password: String, token: String): Future[Boolean] = Future.successful {
    val list = users.filter {
      case (_, values) => values.contains("resetToken") && values("resetToken") == token
    }
    if (list.size == 1) {
      val (email, _) = list.head
      if (users(email)("resetTime").toLong > System.currentTimeMillis) {
        updateFieldOf(email)("password" -> password)
        removeFieldOf(email, "resetToken")
        removeFieldOf(email, "resetTime")
        true
      } else false
    } else false
  }

  private def updateFieldOf(email: String)(keyValue: (String, String)) = Future.successful {
    for (user <- users.get(email)) {
      users = users.updated(email, user + keyValue)
    }
  }

  private def removeFieldOf(email: String, key: String) = {
    for (user <- users.get(email)) {
      users = users.updated(email, user - key)
    }
  }
}