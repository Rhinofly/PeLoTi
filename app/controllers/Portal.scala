package controllers

import models.repository.MongoUserRepository
import play.api.data.Form
import play.api.data.Form._
import play.api.data.Forms._
import play.api.data.validation
import play.api.mvc.Action
import play.api.mvc.Controller
import service.PortalService
import models.Config
import models.TokenRequest
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class Portal(service: PortalService) extends Controller {

  val requestForm = Form(
    mapping("email" -> email.verifying("Email already used", email => !service.checkEmail(email)),
      "password" -> text.verifying("Password required", password => password.nonEmpty))(TokenRequest)(TokenRequest.unapply))

  def requestToken = Action {
    Ok(views.html.requestToken(requestForm))
  }

  def requestTokenHandler = Action.async { implicit request =>
    Future {
      requestForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.requestToken(formWithErrors)),
        value => {
          try {
            Ok(views.html.receiveToken(service.createUser(value)))
          } catch {
            case e: Exception => Ok(e.getMessage)
          }
        })
    }
  }

  def getToken = Action.async {
    Future {
      service.getToken("").map(token => Ok(views.html.receiveToken(token))).getOrElse(BadRequest(""))
    }
  }
}

object Portal extends Portal(PortalService(new MongoUserRepository(Config.databaseName, "users")))