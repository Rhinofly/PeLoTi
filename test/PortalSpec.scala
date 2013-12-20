import scala.concurrent.Future
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import service.Service
import org.specs2.runner.JUnitRunner
import controllers.Portal


@RunWith(classOf[JUnitRunner])
class PortalSpec extends Specification {
  "Portal" should {
    "request token" in new WithApplication {
      val token = service.generateToken("test@example.com")
      val response = tokenRequest(("email", "test@example.com"))
      status(response) must equalTo(OK)
      contentAsString(response) must contain(s"""<label id="token">$token</label>""")
    }
    
    "request token with invalid email" in new WithApplication {
      val response = tokenRequest(("email", "test"))
      status(response) must equalTo(OK)
      contentAsString(response) must contain("Valid email required")
    }
  }
  
  def service = Service(DummyDatabase)
  def portal = new Portal(service)
  
  def tokenRequest(data: (String, String)): Future[SimpleResult] = {
    portal.receive(FakeRequest(POST, "/receive").withFormUrlEncodedBody(data))
  }
}