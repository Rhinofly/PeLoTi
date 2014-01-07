package models

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError

object Validation {
  def nonEmptyText(error: String): Mapping[String] =
    text verifying (Constraint { text: String =>
      if (text.nonEmpty) Valid else Invalid(Seq(ValidationError(error)))
    })
}