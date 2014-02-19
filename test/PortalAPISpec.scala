package test

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import controllers.{Portal => PortalController}
import models.repository.UserRepository
import play.api.libs.json.JsValue
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.OK
import reactor.Portal
import test.repository.MemoryUserRepository
import controllers.PortalAPI

class PortalAPISpec extends Specification with TestRequest with NoTimeConversions {
  private val email = "test@example.com"
  private val databaseName = "test"

  sequential

  "Portal API" should {

    "have a request token method that responds" >> {
      val tokenParameters = Map("email" -> email, "password" -> "password")
      
      "with the correct status" in {
        val response = executePostAction(portalController.requestTokenHandler, tokenParameters)
        testStatus(response, OK)
      }

      "with a valid token" in {
        val response = tokenRequest("email" -> "email@email.com", "password" -> "password")
        await(portalReaktor.getToken(email)) must beLike {
          case Some(token) => (response \ "token").as[String] === token
          case None => failure(s"Could not find token for $email")
        }
      }

      "with a bad request on duplicate email" in {
        val response = executePostAction(portalController.requestTokenHandler, "email" -> email, "password" -> "password")
        testStatus(response, BAD_REQUEST)
      }

      "with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = executePostAction(portalController.requestTokenHandler, parameters)
          testStatus(response, BAD_REQUEST)
        }
        testBadRequest(tokenParameters - "email")
        testBadRequest(tokenParameters - "password")
      }
      
      "correctly to exceptions in a future" in {
        testStatus(executePostAction(badFuturePortal.requestTokenHandler, tokenParameters), INTERNAL_SERVER_ERROR)
      }

      "correctly to exceptions in the code" in {
        testStatus(executePostAction(badCodePortal.requestTokenHandler, tokenParameters), INTERNAL_SERVER_ERROR)
      }
    }

    "have a reset password method that responds" >> {
      val changeParameters = Map("email" -> "test2@example.com", "oldPassword" -> "password", "newPassword" -> "newPassword")
      
      "with the correct status" in {
        val response = executePostAction(portalController.changePasswordHandler, changeParameters)
        testStatus(response, OK)
      }

      "with the password updated" in {
        changePasswordRequest("email" -> "test2@example.com", "oldPassword" -> "newPassword", "newPassword" -> "password")
        await(portalReaktor.validatePassword("test2@example.com", "password")) must beTrue
        await(portalReaktor.validatePassword("test2@example.com", "invalidPassword")) must beFalse
      }

      "with a bad request when missing parameters" in {
        val fakeParameters = Map("email" -> "test2@example.com", "oldPassword" -> "fakePassword", "newPassword" -> "fakePassword")
        def testBadRequest(parameters: Map[String, String]) = {
          val response = executePostAction(portalController.changePasswordHandler, parameters)
          testStatus(response, BAD_REQUEST)
        }

        testBadRequest(fakeParameters)
        testBadRequest(changeParameters - "newPassword")
        testBadRequest(changeParameters - "oldPassword")
        testBadRequest(changeParameters - "email")
      }
      
      "correctly to exceptions in a future" in {
        testStatus(executePostAction(badFuturePortal.changePasswordHandler, changeParameters), INTERNAL_SERVER_ERROR)
      }

      "correctly to exceptions in the code" in {
        testStatus(executePostAction(badCodePortal.changePasswordHandler, changeParameters), INTERNAL_SERVER_ERROR)
      }
    }

    "have a request password reset method that responds" >> {

      "with the correct status" in {
        val response = executePostAction(portalController.requestResetHandler, "email" -> email)
        testStatus(response, OK)
      }

      "with a bad request on invalid parameters" in {
        val response = executePostAction(portalController.requestResetHandler, "field" -> "value")
        testStatus(response, BAD_REQUEST)
      }
      
      "correctly to exceptions in a future" in {
        testStatus(executePostAction(badFuturePortal.requestResetHandler, "email" -> email), INTERNAL_SERVER_ERROR)
      }

      "correctly to exceptions in the code" in {
        testStatus(executePostAction(badCodePortal.requestResetHandler, "email" -> email), INTERNAL_SERVER_ERROR)
      }
    }

    "have a password reset method that responds" >> {
      val resetParameters = Map("password" -> "newPassword", "token" -> "token")
      
      "with the correct status" in {
        val response = executePostAction(portalController.resetPasswordHandler, resetParameters)
        testStatus(response, OK)
      }

      "with the password updated" in {
        executePostAction(portalController.requestResetHandler, "email" -> email)
        resetPasswordRequest("password" -> "password", "token" -> "token")
        await(portalReaktor.validatePassword("test2@example.com", "password")) must beTrue
        await(portalReaktor.validatePassword("test2@example.com", "invalidPassword")) must beFalse
      }

      "with a bad request when missing parameters" in {
        def testBadRequest(parameters: Map[String, String]) = {
          val response = executePostAction(portalController.resetPasswordHandler, parameters)
          testStatus(response, BAD_REQUEST)
        }
        testBadRequest(resetParameters - "token")
        testBadRequest(resetParameters - "password")
      }
      
      "correctly to exceptions in a future" in {
        testStatus(executePostAction(badFuturePortal.resetPasswordHandler, resetParameters), INTERNAL_SERVER_ERROR)
      }

      "correctly to exceptions in the code" in {
        testStatus(executePostAction(badCodePortal.resetPasswordHandler, resetParameters), INTERNAL_SERVER_ERROR)
      }
    }
  }

  private def tokenRequest(body: (String, String)*): JsValue =
    contentAsJson(executePostAction(portalController.requestTokenHandler, body: _*))

  private def changePasswordRequest(body: (String, String)*): JsValue =
    contentAsJson(executePostAction(portalController.changePasswordHandler, body: _*))

  private def requestResetRequest(body: (String, String)*): JsValue =
    contentAsJson(executePostAction(portalController.requestResetHandler, body: _*))

  private def resetPasswordRequest(body: (String, String)*): JsValue =
    contentAsJson(executePostAction(portalController.resetPasswordHandler, body: _*))

  def await[A](func: => Future[A]): A = Await.result(func, 10 seconds)

  val brokenRepositoryException = new RuntimeException("Broken repository test")

  lazy val badFuturePortal = brokenPortal(badRepository(Future.failed(brokenRepositoryException)))
  lazy val badCodePortal = brokenPortal(badRepository(throw brokenRepositoryException))
  def brokenPortal(repository: UserRepository) = new PortalAPI(new Portal(repository, encryption))

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
  
  val userRepository = new MemoryUserRepository
  val encryption = new TestEncryption()
  val portalReaktor = new Portal(userRepository, encryption)
  val portalController = new PortalAPI(portalReaktor)

}