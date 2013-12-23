package service

import play.api.libs.Codecs

class PortalService {
    
  def generateToken(email: String): String = {
    Codecs.md5(email.getBytes)
  }
}

object PortalService {
  def apply: PortalService = {
    new PortalService
  }
}