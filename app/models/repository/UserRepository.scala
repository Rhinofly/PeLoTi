package models.repository

trait UserRepository {
  def create(email: String, password: String, token: String)
  def getToken(email: String): Option[String]
  def exists(email: String): Boolean
  def getPassword(email: String): Option[String]
  def changePassword(email: String, newPassword: String)
}