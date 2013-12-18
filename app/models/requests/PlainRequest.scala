package models.requests

import play.api.libs.json.Json

case class PlainRequest(latitude: Double, longitude: Double, token: String)


object PlainRequest {
  implicit val reads = Json.reads[PlainRequest]
} 