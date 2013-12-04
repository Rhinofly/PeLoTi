import scala.concurrent.Future
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.JsValue
import play.api.mvc.SimpleResult
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

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

    "create a user" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23)
      val create = requestJson(json, "/create")
      contentType(create) must beSome("application/json")
      val result = Json.parse(contentAsString(create))
      (result \ "status").as[String] must contain("OK")
    }
    
    "should find new user" in new WithApplication {
      val json = Json.obj("longitude" -> 54.2, "latitude" -> 5.2)
      val search = requestJson(json, "/search")
      status(search) must equalTo(OK)
      contentType(search) must beSome("application/json")
      val result = Json.parse(contentAsString(search))
      (result \ "people").as[List[JsValue]].length must beEqualTo(5)
    }
    
    "new users should get diffrent id" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23)
      val create1 = requestJson(json, "/create")
      val create2 = requestJson(json, "/create")
      println(contentAsString(create1))
      println(contentAsString(create2))
      contentType(create1) must beSome("application/json")
      contentType(create2) must beSome("application/json")
      val result1 = Json.parse(contentAsString(create1))
      val result2 = Json.parse(contentAsString(create2))
      
      (result1 \ "status").as[String] must contain("OK")
      (result2 \ "status").as[String] must contain("OK")
      (result1 \ "id").as[Long] mustNotEqual((result2 \ "id").as[Long])
    }
  }
}
