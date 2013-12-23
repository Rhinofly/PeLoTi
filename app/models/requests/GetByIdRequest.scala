package models.requests

import play.api.libs.json.Json

case class GetByIdRequest(id: String, token: String)

object GetByIdRequest {
  implicit val getReads = Json.reads[GetByIdRequest]
}