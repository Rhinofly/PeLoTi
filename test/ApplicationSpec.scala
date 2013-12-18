import scala.concurrent.Future
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import controllers.Application
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import service.Service
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Service" should {

    "find users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 54.2, "latitude" -> 5.2, "token" -> "string1")
      val response = Json.parse(searchRequest(json, OK))
      (response \ "people").as[List[JsValue]].length must beEqualTo(4)
    }

    "find no users within region" in new WithApplication {
      val json = Json.obj("longitude" -> 24.4, "latitude" -> 3.2, "token" -> "string1")
      val response = Json.parse(searchRequest(json, OK))
      (response \ "people").as[List[JsValue]].length must beEqualTo(0)
    }

    "send BadRequest on invalid JSON" in new WithApplication {
      val json = Json.obj("invalid" -> "request")
      val response = searchRequest(json, BAD_REQUEST)
      response must contain("error.path.missing")
    }

    "send BadRequest on invalid datatype" in new WithApplication {
      val xml = <location><latitude>54.2</latitude><longitude>5.2</longitude></location>
      val search = route(FakeRequest(POST, "/search").withXmlBody(xml)).get
      status(search) must equalTo(BAD_REQUEST)
    }
    
    "create a user" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23, "token" -> "string1")
      val response = Json.parse(createRequest(json, OK))
      (response \ "status").as[Int] must beEqualTo(OK)
      val response2 = Json.parse(searchRequest(json, OK))
      (response2 \ "people").as[List[JsValue]].length must beEqualTo(5)
    }

    "new users should get diffrent id" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23, "token" -> "string1")
      val response1 = Json.parse(createRequest(json, OK))
      val response2 = Json.parse(createRequest(json, OK))
      (response1 \ "id").as[String] mustNotEqual ((response2 \ "id").as[String])
    }
    
    "a search request should not contain persons from other API clients" in new WithApplication {
      val json = Json.obj("longitude" -> 54.24, "latitude" -> 5.23, "token" -> "token")
      val response = Json.parse(searchRequest(json, OK))
      (response \ "people").as[List[JsValue]].length must beEqualTo(0)
    }
    
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
    
    "update a existing user" in new WithApplication {
      val json = Json.obj("id" -> "1", "longitude" -> 51.04, "latitude" -> 4.21, "token" -> "string1")
      val response = updateRequest(json, OK)
      val jsonResponse = Json.parse(response)
      (jsonResponse \ "status").as[Int] must beEqualTo(OK)
      val json2 = Json.obj("longitude" -> 54.2, "latitude" -> 5.2, "token" -> "string1")
      val response2 = Json.parse(searchRequest(json2, OK))
      (response2 \ "people").as[List[JsValue]].length must beEqualTo(3)
      
    }
    
    "update a user with invalid data" in new WithApplication {
      val json = Json.obj("id" -> "1337", "longitude" -> 51.04, "latitude" -> 4.21, "token" -> "string1")
      val response = updateRequest(json, OK)
      val jsonResponse = Json.parse(response)
      (jsonResponse \ "status").as[Int] must beEqualTo(BAD_REQUEST)
    }
  }
  
  def service = Service(DummyDatabase)
  def application = new Application(service) 
  
  def searchRequest(json: JsObject, status: Int): String = 
    makeRequest(application.search, "/search", json, status)
  def createRequest(json: JsObject, status: Int): String = 
    makeRequest(application.create, "/create", json, status)
  def updateRequest(json: JsObject, status: Int): String =
    makeRequest(application.update, "/update", json, status)
  
  def makeRequest(action: Action[AnyContent], path: String, json: JsObject, htmlStatus: Int): String = {
    val result = action()(FakeRequest(POST, path)
      .withHeaders(HeaderNames.CONTENT_TYPE -> "text/json")
      .withJsonBody(json))
    status(result) must equalTo(htmlStatus)
    contentType(result) must beSome("application/json")
    contentAsString(result)
  }
  
  def tokenRequest(data: (String, String)): Future[SimpleResult] = {
    application.receive(FakeRequest(POST, "/receive").withFormUrlEncodedBody(data))
  }
}
