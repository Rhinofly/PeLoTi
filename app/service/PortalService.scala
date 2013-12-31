package service

import play.api.libs.Codecs
import models._
import models.repository.UserRepository

class PortalService(repository: UserRepository) {
    
  def createUser(request: TokenRequest): String = {
    val token = generateToken(request.email.getBytes, request.password.getBytes)
    repository.create(request.email, request.password, token)
    token
  }
  
  def getToken(email: String): Option[String] = {
    repository.getToken(email)
  }
  
  def checkEmail(email: String): Boolean = {
    repository.exists(email)
  }
  
  def generateToken(emailBytes: Array[Byte], passwordBytes: Array[Byte]): String = {
    Codecs.md5(emailBytes ++ passwordBytes)
  }
}

object PortalService {
  def apply(repository: UserRepository): PortalService = {
    new PortalService(repository)
  }
}