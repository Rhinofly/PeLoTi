package models

import play.api.libs.json.Json

case class Person(location: Location, time: Long, id: Option[String])

object Person {
  implicit val format = Json.format[Person]
}