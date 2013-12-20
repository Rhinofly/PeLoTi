package controllers

import models.MongoDB
import play.api.data.Form
import play.api.data.Forms.email
import play.api.mvc.Action
import play.api.mvc.Controller
import service.Service

class Portal(service: Service) extends Controller {

  val requestForm = Form(
    "email" -> email)

  def request = Action {
    Ok(views.html.requestToken(requestForm))
  }

  def receive = Action { implicit request =>
    requestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.requestToken(formWithErrors)),
      value => Ok(views.html.receiveToken(service.generateToken(value))))
  }
}

object Portal extends Portal(Service(MongoDB))
