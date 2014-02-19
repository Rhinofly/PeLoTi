package models.service

import play.api.libs.json.Json

// Represents a person within the service
case class Person(location: Location, time: Long, id: Option[String], extra: Map[String, String] = Map())

object Person {
  implicit val format = Json.format[Person]
}