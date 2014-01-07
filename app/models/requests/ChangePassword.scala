package models.requests

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.email
import models.Validation
import play.api.data.validation._

case class ChangePassword(email: String, oldPassword: String, newPassword: String) extends AsMap {
  override def asMap =
    Map("email" -> Seq(email), "oldPassword" -> Seq(oldPassword), "newPassword" -> Seq(newPassword))
}

object ChangePassword {
  def form = Form(
    mapping("email" -> email,
      "oldPassword" -> Validation.nonEmptyText("error.password.required.old"),
      "newPassword" -> Validation.nonEmptyText("error.password.required.new"))(ChangePassword.apply)(ChangePassword.unapply))
}
