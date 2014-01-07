package models.requests

import models.Validation
import play.api.data.Form
import play.api.data.Forms.email
import play.api.data.Forms.mapping

case class Token(email: String, password: String) extends AsMap {
  override def asMap =
    Map("email" -> Seq(email), "password" -> Seq(password))
}

object Token {
  
  def form = Form(
    mapping("email" -> email,
      "password" -> Validation.nonEmptyText("error.password.required"))(Token.apply)(Token.unapply))
  
  def form(validate: String => Boolean) = Form(
    mapping("email" -> email.verifying("error.email.used", email => validate(email)),
      "password" -> Validation.nonEmptyText("error.password.required"))(Token.apply)(Token.unapply))
}