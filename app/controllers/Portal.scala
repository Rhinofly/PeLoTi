package controllers

import models.repository.MongoUserRepository
import play.api.data.Form
import play.api.data.Form._
import play.api.data.Forms._
import play.api.data.validation
import play.api.mvc.Action
import play.api.mvc.Controller
import service.PortalService
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.{ Success, Failure }
import play.api.mvc.SimpleResult

class Portal(service: PortalService) extends Controller {

  val requestForm = Form(
    mapping("email" -> email.verifying("Email already used", email => !service.checkEmail(email)),
      "password" -> text.verifying("Password required", password => password.nonEmpty))(TokenRequest)(TokenRequest.unapply))

  val changePasswordForm = Form(
    mapping("email" -> email,
      "oldPassword" -> nonEmptyText,
      "newPassword" -> nonEmptyText)(ChangePasswordRequest)(ChangePasswordRequest.unapply))

  def requestToken = Action {
    Ok(views.html.requestToken(requestForm))
  }

  def requestTokenHandler = Action.async { implicit request =>
    Future {
      requestForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.requestToken(formWithErrors)),
        request => {
          try {
            Ok(views.html.receiveToken(service.createUser(request)))
          } catch {
            case e: Exception => BadRequest(e.getMessage)
          }
        })
    }
  }

  def getToken = Action {
    Ok("")
  }

  def changePassword = Action { implicit request =>
    Ok(views.html.changePassword(changePasswordForm))
  }

  def changePasswordHandler = Action.async { implicit request =>
    Future {
      changePasswordForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.changePassword(formWithErrors)),
        request => {
          try {
            Ok(service.changePassword(request))
          } catch { 
            case e: Exception => BadRequest(e.getMessage)
          }
        })
    }
  }
}

object Portal extends Portal(PortalService(new MongoUserRepository(Config.databaseName, "users")))