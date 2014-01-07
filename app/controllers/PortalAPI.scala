package controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import models.RequestHandler
import models.requests.ChangePassword
import models.requests.ResetPassword
import models.requests.Token
import play.api.data.Form
import play.api.data.Forms.email
import play.api.data.Forms.mapping
import play.api.data.Forms.single
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.SimpleResult
import reactor.{ Portal => PortalReactor }
import play.api.mvc.RequestHeader

class PortalAPI(service: PortalReactor) extends Controller with RequestHandler {

  //TODO: remove this and create utility for async validation
  def await[T](future: Future[T]): T =
    Await.result(future, 1.second)

  val tokenForm = Token.form
  val changePasswordForm = ChangePassword.form
  val resetPasswordForm = ResetPassword.form

  def requestTokenHandler = Action.async { implicit request =>
    tryWithRecover {
      validateForm(tokenForm, {
        request: Token =>
          {
            service.emailExists(request.email).flatMap(exists => exists match {
              case true => Future.successful(BadRequest(error(Messages("error.email.used"), BAD_REQUEST)))
              case false => service.createUser(request).map(token => Ok(response(OK, "token" -> token)))
            })
          }
      })
    }
  }

  def getToken(email: String) = Action.async { implicit request =>
    tryWithRecover {
      service.getToken(email).map {
        case Some(token) => Ok(response(OK, "token" -> token))
        case None => BadRequest(error(Messages("error.email.none"), BAD_REQUEST))
      }
    }
  }

  def changePasswordHandler = Action.async { implicit request =>
    tryWithRecover {
      validateForm(changePasswordForm, {
        request: ChangePassword =>
          service.changePassword(request).map {
            case true => Ok(response(OK, "message" -> Messages("message.password.change.success")))
            case false => BadRequest(error(Messages("message.password.change.error"), BAD_REQUEST))
          }
      })
    }
  }

  val requestResetForm = Form(
    single("email" -> email.verifying("error.email.none", email => await(service.emailExists(email)))))

  def requestResetHandler = Action.async { implicit request =>
    tryWithRecover {
      validateForm(requestResetForm, {
        email: String =>
          service.requestReset(email).map(token => Ok(response(OK, "message" -> Messages("message.mail.send", email))))
      })
    }
  }

  def resetPasswordHandler = Action.async { implicit request =>
    tryWithRecover {
      validateForm(resetPasswordForm, {
        request: ResetPassword =>
          service.resetPassword(request).map {
            case true => Ok(response(OK, "message" -> Messages("message.password.reset.success")))
            case false => BadRequest(error(Messages("message.password.reset.failed"), BAD_REQUEST))
          }
      })
    }
  }

  private def validateForm[T](form: Form[T], function: T => Future[SimpleResult])(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    form.bindFromRequest.fold(
      formWithErrors => formErrors(formWithErrors),
      request => function(request))
  }
}