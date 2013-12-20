package models

import play.api.libs.json.Json

case class Person(id: String, location: List[Double], token: String)

object Person {
  implicit val format = Json.format[Person]
  
  
}