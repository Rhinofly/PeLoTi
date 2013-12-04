import scala.concurrent.Future
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import controllers.Application
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc.SimpleResult
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import controllers.Application
import service.Service
import play.api.mvc.Action
import play.api.http.HeaderNames
import play.api.mvc.AnyContent
import controllers.Application
import service.Service
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.WithApplication
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.SimpleResult
import play.api.test._
import play.api.test.Helpers._
import play.api.test.Helpers.writeableOf_AnyContentAsXml
import service.Service
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Service" should {

    "find users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 54.2, "latitude" -> 5.2, "token" -> "string1")
      val response = requestJsonSearch(json)
      (response \ "people").as[List[JsValue]].length must beEqualTo(4)
    }

    "find no users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 24.4, "latitude" -> 3.2, "token" -> "string1")
      val response = requestJsonSearch(json)
      (response \ "people").as[List[JsValue]].length must beEqualTo(0)
    }
    
    "send BadRequest on invalid JSON" in new WithApplication {
      val json = Json.obj("invalid" -> "request")
      val response = failingSearchRequest(json)
      contentAsString(response) must contain("error.path.missing")
    }

    "send BadRequest on invalid datatype" in new WithApplication {
      val xml = <location><latitude>54.2</latitude><longitude>5.2</longitude></location>
      val search = route(FakeRequest(POST, "/search").withXmlBody(xml)).get
      status(search) must equalTo(BAD_REQUEST)
    }
    
    "create a user" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23)
      val response = requestJsonCreate(json)
      (response \ "status").as[Int] must beEqualTo(OK)
      val response2 = requestJsonSearch(json)
      (response2 \ "people").as[List[JsValue]].length must beEqualTo(5)
    }

    "new users should get diffrent id" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23)
      val response1 = requestJsonCreate(json)
      val response2 = requestJsonCreate(json)
      (response1 \ "id").as[String] mustNotEqual ((response2 \ "id").as[String])
    }
    
    "a search request should not contain persons from other clients" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23, "token" -> "token")
      val response = requestJsonSearch(json)
      (response \ "people").as[List[JsValue]].length must beEqualTo(0)
    }
    
    "request token" in new WithApplication {
      val response = tokenRequest(("email", "test@example.com"))
      status(response) must equalTo(OK)
      contentAsString(response) must contain("""<label id="token">""")
    }
    
    "request token with invalid email" in new WithApplication {
      val response = tokenRequest(("email", "test"))
      status(response) must equalTo(BAD_REQUEST)
      contentAsString(response) must contain("Email is required!")
    }
  }
  
  def application = new Application(new Service(DummyDatabase)) 
  
  def requestJsonSearch(json: JsObject): JsValue = successRequest(application.search, "/search", json)
  def requestJsonCreate(json: JsObject): JsValue = successRequest(application.create, "/create", json)
  
  def failingSearchRequest(json: JsObject): Future[SimpleResult] = 
    failingRequest(application.search, "/search", json)
  
  def successRequest(action: Action[AnyContent], path: String, json: JsObject): JsValue = {
    val result = action()(FakeRequest(POST, path)
      .withHeaders(HeaderNames.CONTENT_TYPE -> "text/json")
      .withJsonBody(json))
    status(result) must equalTo(OK)
    contentType(result) must beSome("application/json")
    Json.parse(contentAsString(result))
  }
  
  def failingRequest(action: Action[AnyContent], path: String, json: JsObject): Future[SimpleResult] = {
    val result = action()(FakeRequest(POST, path)
      .withHeaders(HeaderNames.CONTENT_TYPE -> "text/json")
      .withJsonBody(json))
    status(result) must equalTo(BAD_REQUEST)
    contentType(result) must beSome("application/json")
    result
  }
  
  def tokenRequest(data: (String, String)): Future[SimpleResult] = {
    application.receive(FakeRequest(POST, "/receive")
        .withFormUrlEncodedBody(data))
  }
}
