package test

import models.Encryption

class TestEncryption extends Encryption {
  override def generateToken(parts: String*): String = "token"
  override def encryptPassword(password: String): String = password
  override def checkPassword(password: String, hash: String): Boolean = password == hash
}