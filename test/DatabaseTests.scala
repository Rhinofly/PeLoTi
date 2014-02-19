package test

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.{global => ExecutionGlobal}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import models.repository.PersonRepository
import models.repository.UserRepository
import models.service.Location
import models.service.Person
import play.api.libs.json.JsValue
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.test.Helpers.OK
import reactor.Portal

trait DatabaseTests { self: Specification with NoTimeConversions =>
  sequential
  
  def personRepository: PersonRepository
  def userRepository: UserRepository

  def personRepositoryTests = {
    "get a list of persons by location" in getListOfPersonsByLocation
    "get a list of persons by time" in getListofPersonsByTime
    "get a list of persons by location and time" in getListofPersonsByLocationAndtime
    "create a person" in createPerson
    "be able to get a person by id" in getPersonById
    "handle invalid id request" in getPersonByInvalidId
    "update a person" in updatePerson
  }

  def userRepositoryTests = {
    
    "be able to create a user" in createUser
    "throw an exception on duplicate email" in createDuplicateUser
    "be able to change a password" in changePassword
    "be able to reset a password" in resetPassword
  }

  def getListOfPersonsByLocation =
    await(byLocation).size must equalTo(4)

  def getListofPersonsByTime =
    await(personRepository.getByTime(156643413L, Option(156643414L))).size must equalTo(3)

  def getListofPersonsByLocationAndtime =
    await(personRepository.getByLocationAndTime(Location(54.2, 5.2), 10, 156643413L, None)).size must equalTo(2)

  def createPerson = {
    val person = new Person(Location(54.2, 5.2), 156643413L, None)
    id = await(personRepository.save(person)).id.get
    await(byLocation).size must equalTo(5)
  }

  def getPersonById = {
    val person = await(personRepository.getById(id)).get
    person.location must beEqualTo(new Location(54.2, 5.2))
  }

  def getPersonByInvalidId =
    await(personRepository.getById("id")) should throwA[Exception]

  def updatePerson = {
    val person = new Person(Location(56.2, 5.1), 156643413L, Option(id))
    personRepository.save(person)
    await(byLocation).size must equalTo(4)
  }

  def createUser =
    await(userRepository.create(email, password, "token")) must not(throwA[Exception])

  def createDuplicateUser =
    await(userRepository.create(email, password, "fake")) must (throwA[Exception])

  def changePassword = {
    await(userRepository.changePassword(email, "newPassword"))
    val response = userRepository.getPasswordHash(email)
    response must beSome("newPassword").await
  }

  def resetPassword = {
   await(portalService.requestReset(email))
   userRepository.resetPassword("newPassword2", "token")
   userRepository.getPasswordHash(email) must beSome("newPassword2").await
  }

  def getById(port: Int, id: String, status: Int = OK) =
    getRequest(port, "getById", status, "id" -> id)

  def getRequest(port: Int, path: String, status: Int, parameters: (String, String)*): JsValue =
    request(url(port, path).withQueryString(parameters: _*).get, status) {
      parseResponse(status)
    }

  def url(port: Int, url: String) = WS.url(s"http://localhost:$port/$url")

  def request[T](future: Future[Response], status: Int)(handleResponse: Response => T) = {
    val response = Await.result(future, 10.seconds)
    response.status === status
    handleResponse(response)
  }

  def await[A](func: => Future[A]): A = Await.result(func, 10 seconds)

  def parseResponse(status: Int)(response: Response): JsValue = {
    (response.json \ "status").as[Int] must beEqualTo(status)
    response.json
  }

  
  private def byLocation = personRepository.getByLocation(Location(54.2, 5.2), 20) 
  private val databaseName = "test"
  private val email = "test@example.com"
  private val password = "password"
  private def encryption = new TestEncryption() 
  private lazy val portalService = new Portal(userRepository, encryption)
  private var id = ""
}