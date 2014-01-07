package test

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import controllers.Portal
import global.Global
import models.repository.UserRepository
import play.api.mvc.SimpleResult
import play.api.test.FakeApplication
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.WithServer
import reactor.{ Portal => PortalReactor }
import test.repository.MemoryPersonRepository
import test.repository.MemoryUserRepository
import org.specs2.specification.AroundExample
import org.specs2.execute.Result

class PortalSpec extends Specification with TestRequest with NoTimeConversions {
  private val email = "test1@example.com"
  private val email2 = "test3@example.com"
  private val databaseName = "test"

  sequential

  "Portal" should {

    "have a request token method that responds" >> {
      val tokenParameters = Map("email" -> email, "password" -> "password")

      "with the correct status" in new TestServer {
        val response = tokenRequest(tokenParameters)
        status(response) === OK
      }

      "with a valid token" in new TestServer {
        val response = contentAsString(tokenRequest(Map("email" -> email2, "password" -> "password")))
        await(portalReaktor.getToken(email2)) must beLike {
          case Some(token) => response must contain(token)
          case None => failure(s"Could not find token for $email2")
        }
      }

      "with a bad request on duplicate email" in new TestServer {
        val response = tokenRequest(Map("email" -> email, "password" -> "password"))
        status(response) === BAD_REQUEST
      }

      "with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = tokenRequest(parameters)
          status(response) === BAD_REQUEST
        }
        testBadRequest(tokenParameters - "email")
        testBadRequest(tokenParameters - "password")
      }

      "correctly to exceptions in a future" in new BrokenTestServer(badFutureRepository) {
        status(tokenRequest(tokenParameters)) === INTERNAL_SERVER_ERROR
      }

      "correctly to exceptions in the code" in new BrokenTestServer(badCodeRepository) {
        status(tokenRequest(tokenParameters)) === INTERNAL_SERVER_ERROR
      }
    }

    "have a change password method that responds" >> {
      val changeParameters = Map("email" -> "test2@example.com", "oldPassword" -> "password", "newPassword" -> "newPassword")

      "with the correct status" in new TestServer {
        val response = changePasswordRequest(changeParameters)
        status(response) === OK
      }

      "with the password updated" in new TestServer {
        await(changePasswordRequest(Map("email" -> "test2@example.com", "oldPassword" -> "newPassword", "newPassword" -> "password")))
        await(portalReaktor.validatePassword("test2@example.com", "password")) must beTrue
        await(portalReaktor.validatePassword("test2@example.com", "invalidPassword")) must beFalse
      }

      "with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = changePasswordRequest(parameters)
          status(response) === BAD_REQUEST
        }

        testBadRequest(changeParameters - "newPassword")
        testBadRequest(changeParameters - "oldPassword")
        testBadRequest(changeParameters - "email")
      }

      "correctly to exceptions in a future" in new BrokenTestServer(badFutureRepository) {
        status(changePasswordRequest(changeParameters)) === INTERNAL_SERVER_ERROR
      }

      "correctly to exceptions in the code" in new BrokenTestServer(badCodeRepository) {
        status(changePasswordRequest(changeParameters)) === INTERNAL_SERVER_ERROR
      }
    }

    "have a request password reset method that responds" >> {
      val defaultParameters = Map("email" -> email)

      "with the correct status" in new TestServer {
        val response = requestResetRequest(defaultParameters)
        status(response) === OK
      }

      "with a bad request on invalid parameters" in {
        val response = requestResetRequest(Map("field" -> "value"))
        status(response) === BAD_REQUEST
      }

      "correctly to exceptions in a future" in new BrokenTestServer(badFutureRepository) {
        status(requestResetRequest(defaultParameters)) === INTERNAL_SERVER_ERROR
      }

      "correctly to exceptions in the code" in new BrokenTestServer(badCodeRepository) {
        status(requestResetRequest(defaultParameters)) === INTERNAL_SERVER_ERROR
      }
    }

    "have a password reset method that responds" >> {
      val resetParameters = Map("password" -> "newPassword", "token" -> "token")

      "with the correct status" in new TestServer  {
        val response = resetPasswordRequest(resetParameters)
        status(response) === OK
      }

      "with the password updated" in new TestServer {
        await(requestResetRequest(Map("email" -> "test2@example.com")))
        await(resetPasswordRequest(Map("password" -> "password", "token" -> "token")))
        await(portalReaktor.validatePassword("test2@example.com", "password")) must beTrue
        await(portalReaktor.validatePassword("test2@example.com", "invalidPassword")) must beFalse
      }

      "with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = resetPasswordRequest(parameters)
          status(response) === BAD_REQUEST
        }
        testBadRequest(resetParameters - "token")
        testBadRequest(resetParameters - "password")
      }

      "correctly to exceptions in a future" in new BrokenTestServer(badFutureRepository) {
        status(resetPasswordRequest(resetParameters)) === INTERNAL_SERVER_ERROR
      }

      "correctly to exceptions in the code" in new BrokenTestServer(badCodeRepository) {
        status(resetPasswordRequest(resetParameters)) === INTERNAL_SERVER_ERROR
      }
    }
  }

  private def tokenRequest(body: Map[String, String]): Future[SimpleResult] =
    executePostAction(portalController.requestTokenHandler, body)

  private def changePasswordRequest(body: Map[String, String]): Future[SimpleResult] =
    executePostAction(portalController.changePasswordHandler, body)

  private def requestResetRequest(body: Map[String, String]): Future[SimpleResult] =
    executePostAction(portalController.requestResetHandler, body)

  private def resetPasswordRequest(body: Map[String, String]): Future[SimpleResult] =
    executePostAction(portalController.resetPasswordHandler, body)

  def await[A](func: => Future[A]): A = Await.result(func, 10 seconds)

  val brokenRepositoryException = new RuntimeException("Broken repository test")

  lazy val badFutureRepository = badRepository(Future.failed(brokenRepositoryException))
  lazy val badCodeRepository = badRepository(throw brokenRepositoryException)

  def badRepository(broken: => Future[Nothing]): UserRepository =
    new UserRepository {
      def create(email: String, password: String, token: String) = broken
      def getToken(email: String) = broken
      def exists(email: String) = broken
      def getPasswordHash(email: String) = broken
      def changePassword(email: String, newPassword: String) = broken
      def setResetPassword(email: String, token: String) = broken
      def resetPassword(password: String, token: String) = broken
    }

  val port = 9700
  val userRepository = new MemoryUserRepository
  val encryption = new TestEncryption
  val portalReaktor = new PortalReactor(userRepository, encryption)
  val portalController = new Portal(s"localhost:$port")
  val config = Map("peloti.url" -> s"localhost:$port")

  val application = new FakeApplication(additionalConfiguration = config,
    withGlobal = Option(global(userRepository)))

  def global(userRepository: UserRepository) =
    new Global(_ => Future.successful(new MemoryPersonRepository), _ => userRepository, encryption)

  def brokenApplication(userRepository: UserRepository) =
    new FakeApplication(additionalConfiguration = config,
      withGlobal = Option(global(userRepository)))

  abstract class BrokenTestServer(userRepository: UserRepository) 
  	extends WithServer(brokenApplication(userRepository), port)
  
  abstract class TestServer extends WithServer(application, port)
}