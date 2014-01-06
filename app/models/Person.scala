package models

import play.api.libs.json.Json

case class Person(location: Location, time: Long, id: Option[String], extra: Option[Map[String, String]])

object Person {
  implicit val format = Json.format[Person]
}