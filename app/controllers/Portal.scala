package controllers

import models.MongoDB
import play.api.data.Form
import play.api.data.Forms.email
import play.api.mvc.Action
import play.api.mvc.Controller
import service.PortalService
import models.Config

class Portal(service: PortalService) extends Controller {

  val requestForm = Form(
    "email" -> email)

  def requestToken = Action {
    Ok(views.html.requestToken(requestForm))
  }

  def requestTokenHandler = Action { implicit request =>
    requestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.requestToken(formWithErrors)),
      value => Ok(views.html.receiveToken(service.generateToken(value))))
  }
}

object Portal extends Portal(PortalService.apply)