package models

import scala.collection.Seq
import play.api.libs.Codecs
import org.mindrot.jbcrypt.BCrypt

class BlowfishEncryption extends Encryption {
  override def generateToken(parts: String*): String =
    Codecs.md5(parts.mkString.getBytes)
 
  override def encryptPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))
  override def checkPassword(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}