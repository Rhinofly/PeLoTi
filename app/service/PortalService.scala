package service

import play.api.libs.Codecs
import org.mindrot.jbcrypt.BCrypt
import models._
import models.repository.UserRepository
import scala.concurrent.Await
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.Success

class PortalService(repository: UserRepository) {

  def createUser(request: TokenRequest): String = {
    val token = generateToken(request.email.getBytes, request.password.getBytes)
    val encryptedPassword = encryptPassword(request.password)
    repository.create(request.email, encryptedPassword, token)
    token
  }

  def getToken(email: String): Option[String] = {
    repository.getToken(email)
  }

  def checkEmail(email: String): Boolean = {
    repository.exists(email)
  }

  def changePassword(request: ChangePasswordRequest): String = {
    repository.getPassword(request.email) match {
      case Some(hash) => {
        if (checkPassword(request.oldPassword, hash)) {
          repository.changePassword(request.email, encryptPassword(request.newPassword))
          "You have succesfully changed your password"
        } else
          throw new Exception("Invalid email or password")
      }
      case None => throw new Exception("Invalid email or password")
    }
  }

  def generateToken(emailBytes: Array[Byte], passwordBytes: Array[Byte]): String = {
    Codecs.md5(emailBytes ++ passwordBytes)
  }

  def encryptPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt(12))
  }

  def checkPassword(password: String, hash: String): Boolean = {
    BCrypt.checkpw(password, hash)
  }
}

object PortalService {
  def apply(repository: UserRepository): PortalService = {
    new PortalService(repository)
  }
}