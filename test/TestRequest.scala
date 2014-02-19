package test

import scala.concurrent.Future

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.test.Helpers.POST
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status

trait TestRequest { self: Specification with NoTimeConversions =>

  def executePostAction(action: Action[AnyContent], body: (String, String)*): Future[SimpleResult] =
    action(FakeRequest(POST, "").withFormUrlEncodedBody(body: _*))

  def executePostAction(action: Action[AnyContent], body: Map[String, String]): Future[SimpleResult] =
    executePostAction(action, body.toSeq: _*)

  def executeGetAction(action: Action[AnyContent]): Future[SimpleResult] =
    action(FakeRequest(GET, ""))

  def contentAsJson(response: Future[SimpleResult]): JsValue =
    Json.parse(contentAsString(response))

  def testStatus(response: Future[SimpleResult], statusCode: Int) = {
    val json = Json.parse(contentAsString(response))
    (status(response) === statusCode) and
      ((json \ "status").as[Int] === statusCode)
  }
}