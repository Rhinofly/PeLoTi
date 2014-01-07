package models.requests

import models.Validation
import play.api.data.Form
import play.api.data.Forms.mapping

case class ResetPassword(password: String, token: String) extends AsMap {
  override def asMap =
    Map("password" -> Seq(password), "token" -> Seq(token))
}

object ResetPassword {
  def form = Form(
    mapping(
      "password" -> Validation.nonEmptyText("error.password.required"),
      "token" -> Validation.nonEmptyText("error.token.required"))(ResetPassword.apply)(ResetPassword.unapply))
}