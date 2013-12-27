package models.requests

import play.api.libs.json.Json

case class GetByLocationRequest(latitude: Double, longitude: Double, token: String)

object GetByLocationRequest {
  implicit val reads = Json.reads[GetByLocationRequest]
}