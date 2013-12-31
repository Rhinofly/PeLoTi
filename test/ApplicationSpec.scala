import scala.annotation.implicitNotFound
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import com.mongodb.casbah.Imports.MongoDBObject
import controllers.Application
import models.Config
import models.repository.MongoPersonRepository
import play.api.libs.json.JsValue
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.OK
import play.api.test.WithServer
import service.Service
import models.Person
import models.Location

class ApplicationSpec extends Specification with NoTimeConversions {

  var id = ""
  
  "Service" should {

    "find users within region" in new WithServer {
      val response = getByLocation(port, 54.2, 5.2, 10)
      (response \ "people").as[List[JsValue]].length must beEqualTo(4)
    }

    "find no users within region" in new WithServer {
      val response = getByLocation(port, 24.4, 3.2, 10)
      (response \ "people").as[List[JsValue]].length must beEqualTo(0)
    }

    "send BadRequest on missing parameters" in new WithServer {
      val response = getRequest(port, "getByLocation", BAD_REQUEST, ("latitude" -> "5.2"))
      (response \ "message").as[String] must contain("Missing parameter: longitude")
    }

    "create a user" in new WithServer {
      val body = Map("longitude" -> Seq("54.2"), "latitude" -> Seq("5.2"), "time" -> Seq("156643414"))
      val response = saveRequest(port, body, OK)
      (response \ "status").as[Int] must beEqualTo(OK)
      id = (response \ "id").as[String]
      val response2 = getByLocation(port, 54.2, 5.2, 10)
      (response2 \ "people").as[List[JsValue]].length must beEqualTo(5)
    }

    "new users should get diffrent id" in new WithServer {
      val body = Map("longitude" -> Seq("54.24"), "latitude" -> Seq("5.23"), "token" -> Seq("string1"))
      def response = saveRequest(port, body, OK)
      (response \ "id").as[String] mustNotEqual ((response \ "id").as[String])
    }

    "update a existing user" in new WithServer {
      val body = Map("id" -> Seq(id), "longitude" -> Seq("51.04"), "latitude" -> Seq("5.21"), "time" -> Seq("156643432"))
      val response = saveRequest(port, body)
      val response2 = getByLocation(port, 54.2, 5.2, 10)
      (response2 \ "people").as[List[JsValue]].length must beEqualTo(4)
    }

    "update a user with invalid data" in new WithServer {
      val body = Map("id" -> Seq(id))
      val response = saveRequest(port, body, BAD_REQUEST)
    }

    "get user by id" in new WithServer {
      val response = getById(port, id)
      (response \ "location" \ "longitude").as[Double] must beEqualTo(51.04)
      (response \ "location" \ "latitude").as[Double] must beEqualTo(5.21)
    }

    "get user by invalid id" in new WithServer {
      val response = getById(port, "test", BAD_REQUEST)
    }
    
    "find users by time" in new WithServer {
      val response = getByTime(port, 156643413L, 156643414L)
      (response \ "people").as[List[JsValue]].length must beEqualTo(4)
    }
    
    "find users by time and location" in new WithServer {
      val response = getByLocationAndTime(port, 54.2, 5.2, 156643413L)
      (response \ "people").as[List[JsValue]].length must beEqualTo(4)
    }
  }

  def database = new MongoPersonRepository(Config.databaseName, "test")
  def service = Service(database)
  def application = new Application(service)

  def getByLocation(port: Int, longitude: Double, latitude: Double, radius: Long, status: Int = OK): JsValue =
    getRequest(port, "getByLocation", status, "longitude" -> String.valueOf(longitude), "latitude" -> String.valueOf(latitude), "radius" -> String.valueOf(radius))
  def saveRequest(port: Int, body: Map[String, Seq[String]], status: Int = OK): JsValue = postRequest(port, "save", status, body)
  def getById(port: Int, id: String, status: Int = OK): JsValue = getRequest(port, "getById", status, "id" -> id)
  def getByTime(port: Int, start: Long, end: Long, status: Int = OK): JsValue = 
    getRequest(port, "getByTime", status, "start" -> String.valueOf(start), "end" -> String.valueOf(end))
  def getByLocationAndTime(port: Int, longitude: Double, latitude: Double, start: Long, status: Int = OK): JsValue =
    getRequest(port, "getByLocationAndTime", status, "longitude" -> String.valueOf(longitude), 
                                                     "latitude" -> String.valueOf(latitude), 
                                                     "start" -> String.valueOf(start))

  def postRequest(port: Int, url: String, status: Int, body: Map[String, Seq[String]]): JsValue = {
    val reponse = WS.url(s"http://localhost:$port/$url").post(body)
    parseResponse(reponse, status)
  }

  def getRequest(port: Int, url: String, status: Int, parameters: (String, String)*): JsValue = {
    val request = WS.url(s"http://localhost:$port/$url").withQueryString(parameters:_*)
    parseResponse(request.get, status)
  }

  def parseResponse(future: Future[Response], status: Int): JsValue = {
    val response = Await.result(future, 10.seconds)
    (response.json \ "status").as[Int] must beEqualTo(status)
    response.json
  }
}