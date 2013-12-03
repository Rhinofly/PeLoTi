package models

import play.api.libs.json.Json

case class Person(Longitude: Double, Latitude: Double)

object Person {
  implicit val format = Json.format[Person]
}