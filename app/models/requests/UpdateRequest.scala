package models.requests

import play.api.libs.json.Json


case class UpdateRequest(latitude: Double, longitude: Double, id: Option[String])

object UpdateRequest {
  implicit val updateReads = Json.reads[UpdateRequest]
} 