package models.repository

import scala.concurrent.Future

trait UserRepository {
  def create(email: String, password: String, token: String):Future[Unit]
  def getToken(email: String): Future[Option[String]]
  def exists(email: String): Future[Boolean]
  def getPasswordHash(email: String): Future[Option[String]]
  def changePassword(email: String, newPassword: String):Future[Unit]
  def setResetPassword(email: String, token: String): Future[Unit]
  def resetPassword(password: String, token: String): Future[Boolean]
}