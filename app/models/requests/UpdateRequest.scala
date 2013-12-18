package models.requests

import play.api.libs.json.Json

case class UpdateRequest(id: String, latitude: Double, longitude: Double, token: String)

object UpdateRequest {
  implicit val reads = Json.reads[UpdateRequest]
}