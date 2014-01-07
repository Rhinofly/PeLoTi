package models

import scala.concurrent.Future
import scala.util.Try
import play.api.data.Form
import play.api.http.Status.OK
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.RequestHeader
import play.api.mvc.Results.Ok
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.InternalServerError
import play.api.mvc.SimpleResult
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.Mapping
import play.api.data.Form
import play.api.data.Forms.email
import play.api.data.Forms.mapping
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import models.service.Person
import play.api.i18n.Lang

trait RequestHandler {

  def error(error: JsValueWrapper, status: Int): JsObject =
    response(status, "message" -> error)

  def response(status: Int, field: (String, JsValueWrapper)): JsObject = {
    Json.obj("status" -> status, field)
  }

  def tryWithRecover(code: => Future[SimpleResult])(implicit request: RequestHeader): Future[SimpleResult] =
    tryWithRecover(code, InternalServerError(error(Messages("error.internal"), INTERNAL_SERVER_ERROR)))
  
  def tryWithRecover(code: => Future[SimpleResult], error: SimpleResult)(implicit request: RequestHeader): Future[SimpleResult] = {
    Try(code)
      .recover {
        case t: Throwable => Future.failed(t)
      }.get
      .recover {
        case t: Throwable =>
          // t.printStackTrace()
          // JiraExceptionProcessor.reportError(request, t)
          // Do not return the details of the exception as it might
          // contain information that should not be seen by the
          // outside world
          error
      }
  }

  def formErrors[T](form: Form[T]) = Future.successful {
    BadRequest(Json.obj("status" -> BAD_REQUEST, "errors" -> form.errorsAsJson(Lang.defaultLang)))
  }
}