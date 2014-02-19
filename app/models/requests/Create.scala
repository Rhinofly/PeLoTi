package models.requests

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.of
import play.api.data.format.Formats.doubleFormat
import play.api.data.format.Formats.longFormat

case class Create(latitude: Double, longitude: Double, time: Long)

object Create {
    def form = Form(
    mapping(
      "longitude" -> of[Double],
      "latitude" -> of[Double],
      "time" -> of[Long])(Create.apply)(Create.unapply))
}