package models

import play.api.libs.json.Json

case class Person(id: String, location: List[Double])

object Person {
  implicit val format = Json.format[Person]
}