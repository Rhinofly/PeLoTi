package models

trait Encryption {
  def generateToken(parts: String*): String
  def encryptPassword(password: String): String
  def checkPassword(password: String, hash: String): Boolean
}