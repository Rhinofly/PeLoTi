package models.requests

import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.of
import play.api.data.Forms.optional
import play.api.data.Mapping
import play.api.data.format.Formats.doubleFormat
import play.api.data.format.Formats.longFormat
import models.Validation

case class Update(id: String, longitude: Option[Double], latitude: Option[Double], time: Option[Long], values: Option[List[ExtraField]])
case class ExtraField(key: String, value: String)

object Update {
    def form = Form(
    mapping(
      "id" -> Validation.nonEmptyText("error.id.required"),
      "longitude" -> optional(of[Double]),
      "latitude" -> optional(of[Double]),
      "time" -> optional(of[Long]),
      "extras" -> optional(list(
        mapping(
          "key" -> Validation.nonEmptyText("error.key.required"),
          "value" -> Validation.nonEmptyText("error.value.required"))(ExtraField)(ExtraField.unapply))))(Update.apply)(Update.unapply))
}