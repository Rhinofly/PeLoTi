import scala.concurrent.Future
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import controllers.Portal
import service.PortalService
import models._
import models.repository._

class PortalSpec extends Specification {
  val email = "test@example.com"
  
  "Portal" should {
    "request token" in new WithApplication {
      val token = service.generateToken(email.getBytes, "password".getBytes)
      val response = tokenRequest("email" -> email, "password" -> "password")
      status(response) must equalTo(OK)
      contentAsString(response) must contain(s"""<label id="token">$token</label>""")
    }
    
    "request token with invalid form" in new WithApplication {
      val response = tokenRequest(("email", "test"))
      status(response) must equalTo(BAD_REQUEST)
    }
    
    "request token with already used email" in new WithApplication {
      val response = tokenRequest("email" -> email, "password" -> "password")
      status(response) must equalTo(BAD_REQUEST)
    }
    
    "should be able to verify password" in new WithApplication {
      val hash = service.encryptPassword("password")
      service.checkPassword("password", hash) must equalTo(true)
    }
    
    "should be able to change password" in new WithApplication {
      val response = changePasswordRequest("email" -> email, "oldPassword" -> "password", "newPassword" -> "newPassword")
      status(response) must equalTo(OK)
    }
  }
  def repository = new MongoUserRepository(Config.databaseName, "test")
  def service = PortalService(repository)
  def portal = new Portal(service)
  
  def tokenRequest(data: (String, String)*): Future[SimpleResult] = {
    portal.requestTokenHandler(FakeRequest(POST, "/receive").withFormUrlEncodedBody(data :_*))
  }
  
  def changePasswordRequest(data: (String, String)*): Future[SimpleResult] = {
    portal.changePassword(FakeRequest(POST, "changePassword").withFormUrlEncodedBody(data :_*))
  }
}