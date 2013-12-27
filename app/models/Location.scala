package models

import play.api.libs.json.Json

case class Location(longitude: Double, latitude: Double)

object Location {
  implicit val format = Json.format[Location]
}