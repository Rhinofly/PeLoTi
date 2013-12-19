package models

import play.api.libs.json.Json

case class Person(id: Long, latitude: Double, longitude: Double)

object Person {
  implicit val format = Json.format[Person]
}