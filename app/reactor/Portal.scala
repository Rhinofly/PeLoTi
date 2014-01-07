package reactor

import scala.concurrent.Future

import models.Encryption
import models.repository.UserRepository
import models.requests.ChangePassword
import models.requests.ResetPassword
import models.requests.Token
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// Holds application logic for Portal
class Portal(repository: UserRepository, encryption: Encryption) {

  def createUser(request: Token): Future[String] = { 
    Future {
    val token = encryption.generateToken(request.email, request.password)
    val encryptedPassword = encryption.encryptPassword(request.password)
    repository.create(request.email, encryptedPassword, token)
    token
    }
  }

  val getToken = repository.getToken _
  val emailExists = repository.exists _

  def changePassword(request: ChangePassword): Future[Boolean] = {
    validatePassword(request.email, request.oldPassword).map { result =>
      if(result) repository.changePassword(request.email, encryption.encryptPassword(request.newPassword))
      result
    }
  }

  def requestReset(email: String): Future[String] = {
    val token = encryption.generateToken(email, String.valueOf(System.currentTimeMillis()))
    repository.setResetPassword(email, token).map(_ => token)
  }
  
  def validatePassword(email: String, password: String): Future[Boolean] = {
    repository.getPasswordHash(email).map {
      case Some(hash) => {
        encryption.checkPassword(password, hash)
      }
      case None => false
    }
  }

  def resetPassword(request: ResetPassword) = {
    val hash = encryption.encryptPassword(request.password)
    repository.resetPassword(hash, request.token)
  }
}