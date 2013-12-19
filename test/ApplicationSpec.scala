import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import scala.concurrent.Future
import play.api.mvc.SimpleResult

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  def requestJson(json: JsObject, path: String): Future[SimpleResult] = {
    route(FakeRequest(POST, path).withJsonBody(json)).get
  }

  "Service" should {

    "find users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 54.2, "latitude" -> 5.2)
      val search = requestJson(json, "/search")
      status(search) must equalTo(OK)
      contentType(search) must beSome("application/json")
      val result = Json.parse(contentAsString(search))
      (result \ "people").as[List[JsValue]].length must beEqualTo(4)
    }

    "find no users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 24.4, "latitude" -> 3.2)
      val search = requestJson(json, "/search")
      status(search) must equalTo(OK)
      contentType(search) must beSome("application/json")
      val result = Json.parse(contentAsString(search))
      (result \ "people").as[List[JsValue]].length must beEqualTo(0)
    }
   
    "BadRequest on invalid JSON" in new WithApplication {
      val json = Json.obj("invalid" -> "request")
      val search = requestJson(json, "/search")
      status(search) must equalTo(BAD_REQUEST)
      contentType(search) must beSome("application/json")
      contentAsString(search) must contain("error.path.missing")
    }
    
    "BadRequest on invalid data" in new WithApplication {
      val xml = <location><latitude>54.2</latitude><longitude>5.2</longitude></location>
      val search = route(FakeRequest(POST, "/search").withXmlBody(xml)).get
      status(search) must equalTo(BAD_REQUEST)
    }
  }
}
