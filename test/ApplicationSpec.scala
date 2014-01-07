// format: +preserveDanglingCloseParenthesis

package test

import scala.concurrent.Future
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import controllers.Service
import models.repository.PersonRepository
import models.service.Location
import models.service.Person
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.GET
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.OK
import play.api.test.Helpers.route
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import play.api.test.WithServer
import reactor.{ Service => reactorService }
import test.repository.MemoryPersonRepository
import play.api.test.FakeApplication
import global.Global
import test.repository.MemoryUserRepository

class ApplicationSpec extends Specification with TestRequest with NoTimeConversions {
  sequential

  private var id = "1"
  private val databaseName = "test"

  "Service" should {

    "be able to find users" >> {

      "by location" in {
        val response = getByLocation(54.2, 5.2, 20)
        (response \ "people").as[List[JsValue]].length must beEqualTo(4)
      }

      "by location without result" in {
        val response = getByLocation(24.4, 3.2, 10)
        (response \ "people").as[List[JsValue]].length must beEqualTo(0)
      }

      "by time" in {
        val response = getByTime(156643413L, 156643414L)
        (response \ "people").as[List[JsValue]].length must beEqualTo(3)
      }

      "by location and time" in {
        val response = getByLocationAndTime(54.2, 5.2, 156643413L)
        (response \ "people").as[List[JsValue]].length must beEqualTo(2)
      }

      "and respond correctly to exceptions in a future" in {
        testStatus(executeGetAction(badFutureService.byLocation(54.2, 5.2, 10)), INTERNAL_SERVER_ERROR)
        testStatus(executeGetAction(badFutureService.getByTime(156643413L, Option(156643414L))), INTERNAL_SERVER_ERROR)
        testStatus(executeGetAction(badFutureService.getByLocationAndTime(54.2, 5.2, 156643413, None)), INTERNAL_SERVER_ERROR)
      }

      "and respond correctly to exceptions in the code" in {
        testStatus(executeGetAction(badCodeService.byLocation(54.2, 5.2, 10)), INTERNAL_SERVER_ERROR)
        testStatus(executeGetAction(badCodeService.getByTime(156643413L, Option(156643414L))), INTERNAL_SERVER_ERROR)
        testStatus(executeGetAction(badCodeService.getByLocationAndTime(54.2, 5.2, 156643413, None)), INTERNAL_SERVER_ERROR)
      }

      "respond with a bad request when missing parameters in a get by location request" in new WithServer(
        FakeApplication(withGlobal = Option(new Global(application => Future.successful(repository),
          application => new MemoryUserRepository, new TestEncryption)))) {
        def testBadRequest(path: String, parameters: String) = {
          val response = route(FakeRequest(GET, s"/$path?$parameters")).get
          testStatus(response, BAD_REQUEST)
        }
        testBadRequest("getByLocation", "latitude=5.2")
        testBadRequest("getByLocation", "longitude=54.2")
        testBadRequest("getByTime", "end=154487454")
        testBadRequest("getByLocationAndTime", "latitude=5.2&longitude=54.2")
        testBadRequest("getByLocationAndTime", "latitude=5.2&start=154487454")
        testBadRequest("getByLocationAndTime", "longitude=54.2&start=154487454")
      }
    }

    "have a create user method that" >> {
      val createParameters = Map("longitude" -> "54.2", "latitude" -> "5.2", "time" -> "156643414")

      "responds with the correct status" in {
        val response = executePostAction(serviceController.create, createParameters)
        testStatus(response, OK)
      }

      "should result in a new location" in {
        create(createParameters.toSeq: _*)
        val response = getByLocation(54.2, 5.2, 10)
        (response \ "people").as[List[JsValue]].length must beEqualTo(4)
      }

      "responds with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = executePostAction(serviceController.create, parameters)
          testStatus(response, BAD_REQUEST)
        }
        testBadRequest(createParameters - "longitude")
        testBadRequest(createParameters - "latitude")
        testBadRequest(createParameters - "time")
      }

      "responds with a valid person" in {
        val response = create(createParameters.toSeq: _*)
        val expectedLocation =
          Json.obj(
            "longitude" -> createParameters("longitude").toDouble,
            "latitude" -> createParameters("latitude").toDouble
          )
        val person = response \ "person"
        val location = person \ "location"
        location === expectedLocation
        (person \ "time").as[Long] === createParameters("time").toLong
        (person \ "id").as[String] must not beEmpty
      }

      "does not duplicate id's" in {
        def createRequest = {
          val response = executePostAction(serviceController.create, createParameters)
          testStatus(response, OK)
          contentAsJson(response)
        }
        val json1 = createRequest
        val json2 = createRequest
        (json1 \ "person" \ "id").as[String] mustNotEqual ((json2 \ "person" \ "id").as[String])
      }

      "and respond correctly to exceptions in a future" in {
        testStatus(executePostAction(badFutureService.create, createParameters), INTERNAL_SERVER_ERROR)
      }

      "and respond correctly to exceptions in the code" in {
        testStatus(executePostAction(badCodeService.create, createParameters), INTERNAL_SERVER_ERROR)
      }
    }

    "update a existing user" >> {
      val updateParameters = Map("id" -> id, "longitude" -> "51.04", "latitude" -> "5.21", "time" -> "156643432")

      "with the correct status" in {
        val response = executePostAction(serviceController.update, updateParameters)
        testStatus(response, OK)
      }

      "update a user with invalid data" in {
        val response = executePostAction(serviceController.update, updateParameters - "id")
        testStatus(response, BAD_REQUEST)
      }

      "should result in location search to be one less" in {
        update(updateParameters)
        val response = getByLocation(54.2, 5.2, 10)
        (response \ "people").as[List[JsValue]].length must beEqualTo(6)
      }

      "should update the correct user" in {
        val json = update(updateParameters)
        (json \ "person" \ "id").as[String] === id
      }
    }

    "get user by id" >> {

      "with the correct status" in {
        val response = executeGetAction(serviceController.byId(id))
        testStatus(response, OK)
      }

      "with expected data" in {
        val response = getById(id)
        (response \ "person" \ "location" \ "longitude").as[Double] must beEqualTo(51.04)
        (response \ "person" \ "location" \ "latitude").as[Double] must beEqualTo(5.21)
      }

      "with a bad request when id is invalid" in {
        val response = executeGetAction(serviceController.byId("1337"))
        testStatus(response, BAD_REQUEST)
      }

      "and respond correctly to exceptions in a future" in {
        testStatus(executeGetAction(badFutureService.byId(id)), INTERNAL_SERVER_ERROR)
      }

      "and respond correctly to exceptions in the code" in {
        testStatus(executeGetAction(badCodeService.byId(id)), INTERNAL_SERVER_ERROR)
      }

    }

    "store extra data on a user" >> {
      val extraParameters = Map("id" -> id, "extras[0].key" -> "key", "extras[0].value" -> "value")

      "with the correct status" in {
        val response = update(extraParameters)
        (response \ "person" \ "extra" \ "key").as[String] must beEqualTo("value")
      }

      "with expected data" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = executePostAction(serviceController.update, parameters)
          testStatus(response, BAD_REQUEST)
        }
        testBadRequest(extraParameters - "extras[0].value")
        testBadRequest(extraParameters - "extras[0].key")
        testBadRequest(extraParameters - "id")
      }

      "should be persistant when updating a user" in {
        val response = update(Map("id" -> "2", "longitude" -> "51.04", "latitude" -> "5.21", "time" -> "156643432"))
        (response \ "person" \ "extra" \ "key").as[String] must beEqualTo("value")
      }
    }
  }

  val brokenRepositoryException = new RuntimeException("Broken repository test")

  def badRepository(broken: => Future[Nothing]): PersonRepository =
    new PersonRepository {
      def getById(id: String) = broken
      def getByLocation(location: Location, radius: Long): Future[List[Person]] = broken
      def getByTime(start: Long, end: Option[Long]): Future[List[Person]] = broken
      def getByLocationAndTime(location: Location, radius: Long, start: Long, end: Option[Long]): Future[List[Person]] = broken
      def save(person: Person): Future[Person] = broken
    }

  def badFutureService = {
    val repository = badRepository(Future.failed(brokenRepositoryException))
    new Service(new reactorService(repository))
  }

  def badCodeService = {
    val repository = badRepository(throw brokenRepositoryException)
    new Service(new reactorService(repository))
  }

  def getByLocation(longitude: Double, latitude: Double, radius: Long): JsValue =
    contentAsJson(executeGetAction(serviceController.byLocation(longitude, latitude, radius)))

  def create(body: (String, String)*): JsValue =
    contentAsJson(executePostAction(serviceController.create, body: _*))

  def update(body: Map[String, String]): JsValue =
    contentAsJson(executePostAction(serviceController.update, body))

  def getById(id: String): JsValue =
    contentAsJson(executeGetAction(serviceController.byId(id)))

  def getByTime(start: Long, end: Long): JsValue =
    contentAsJson(executeGetAction(serviceController.getByTime(start, Option(end))))

  def getByLocationAndTime(longitude: Double, latitude: Double, start: Long): JsValue =
    contentAsJson(executeGetAction(serviceController.getByLocationAndTime(longitude, latitude, start, None)))

  val repository = new MemoryPersonRepository
  val serviceReaktor = new reactorService(repository)
  val serviceController = new Service(serviceReaktor)
}