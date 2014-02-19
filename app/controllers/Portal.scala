package controllers

import scala.concurrent.Future

import models.RequestHandler
import models.requests.AsMap
import models.requests.ChangePassword
import models.requests.ResetPassword
import models.requests.Token
import play.api.data.Form
import play.api.data.Forms.email
import play.api.data.Forms.single
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.SimpleResult
import play.api.templates.Html

//HTML implementation of PortalAPI
class Portal(address: String) extends Controller with RequestHandler {

  val tokenForm = Token.form
  val changePasswordForm = ChangePassword.form
  val requestResetForm = Form(single("email" -> email))
  val resetPasswordForm = ResetPassword.form

  def requestToken = Action {
    Ok(views.html.requestToken(tokenForm))
  }

  def requestTokenHandler = Action.async { implicit request =>
    tryWithRecover {
      handleRequest(tokenForm, views.html.requestToken.apply, "requestToken", { json =>
        val token = (json \ "token").as[String]
        views.html.receiveToken(token)
      })
    }
  }

  def getToken(email: String) = Action.async { implicit request =>
    tryWithRecover {
      url("getToken").withQueryString("email" -> email).get
        .map(handleResponse(_) { json =>
          val token = (json \ "token").as[String]
          views.html.receiveToken(token)
        })
    }
  }

  def changePassword = Action {
    Ok(views.html.changePassword(changePasswordForm))
  }

  def changePasswordHandler = Action.async { implicit request =>
    tryWithRecover {
      handleRequest(changePasswordForm, views.html.changePassword.apply, "changePassword", { json =>
        val message = (json \ "message").as[String]
        views.html.success(message)
      })
    }
  }

  def requestReset = Action {
    Ok(views.html.requestReset(requestResetForm))
  }

  def requestResetHandler = Action.async { implicit request =>
    tryWithRecover {
      requestResetForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.requestReset(formWithErrors))),
        email => postToWebservice("requestReset", Map("email" -> Seq(email))) {
          response =>
            handleResponse(response) { json =>
              val message = (json \ "message").as[String]
              views.html.success(message)
            }
        })
    }
  }

  def resetPassword(token: String) =
    Action(Ok(views.html.resetPassword(resetPasswordForm.fill(new ResetPassword("", token)))))

  def resetPasswordHandler = Action.async { implicit request =>
    tryWithRecover {
      handleRequest(resetPasswordForm, views.html.resetPassword.apply, "resetPassword", { json =>
        val message = (json \ "message").as[String]
        views.html.success(message)
      })
    }
  }

  private def url(path: String) =
    WS.url(s"http://$address/portal/api/$path")

  private def postToWebservice(path: String, parameters: Map[String, Seq[String]])(function: Response => SimpleResult): Future[SimpleResult] =
    url(path).post(parameters)
      .map(response => function(response))

  private def handleResponse(response: Response)(function: JsValue => Html): SimpleResult = {
    response.status match {
      case OK => Ok(function(response.json))
      case BAD_REQUEST => BadRequest(getErrorPage(response.json))
      case INTERNAL_SERVER_ERROR => InternalServerError(getErrorPage(response.json))
    }
  }

  private def getErrorPage(json: JsValue): Html = {
    val message = (json \ "message").as[String]
    views.html.error(message)
  }

  override def tryWithRecover(code: => Future[SimpleResult])(implicit request: RequestHeader): Future[SimpleResult] =
    tryWithRecover(code, InternalServerError(views.html.error(Messages("error.internal"))))

  private def handleRequest[T <: AsMap](form: Form[T], formError: Form[T] => Html, path: String, function: JsValue => Html)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(formError(formWithErrors))),
      T => url(path).post(T.asMap).map { response =>
        handleResponse(response)(function)
      })
  }
}