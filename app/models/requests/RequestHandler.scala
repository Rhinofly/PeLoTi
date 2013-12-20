package models.requests

import play.api.libs.json.Json

case class MainRequest(latitude: Double, longitude: Double, token: String)
case class UpdateRequest(id: String, latitude: Double, longitude: Double, token: String)
case class GetRequest(id: String, token: String)

object RequestHandler {
  implicit val mainReads = Json.reads[MainRequest]
  implicit val updateReads = Json.reads[UpdateRequest]
  implicit val getReads = Json.reads[GetRequest]
} 