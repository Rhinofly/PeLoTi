package reactor

import models._
import play.api.libs.json._
import models.repository._
import play.api.http.Status._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util._
import play.api.libs.json.Json.JsValueWrapper
import models.repository.PersonRepository
import scala.concurrent.duration._
import models.requests._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import models.service.Location
import models.service.Person
import models.requests.Create
import scala.concurrent.Await
import scala.concurrent.duration._
import models.service.Location
import models.service.Person
import models.requests._

// Holds application logic for Application
class Service(repository: PersonRepository) {

  def getByLocation(longitude: Double, latitude: Double, radius: Long = 10): Future[List[Person]] = {
    repository.getByLocation(Location(longitude, latitude), radius)
  }

  def createPerson(request: Create): Future[Person] = {
    repository.save(Person(Location(request.latitude, request.longitude), request.time, None))
  }

  def updatePerson(request: Update): Future[Person] = {
    repository.getById(request.id).map { optionPerson =>
      val person = optionPerson.get
      val location = (for (longitude <- request.longitude; latitude <- request.latitude) yield Location(longitude, latitude))
        .getOrElse(person.location)
      val time = request.time.getOrElse(person.time)
      val extra = request.values.map(_.map(field => field.key -> field.value).toMap).getOrElse(person.extra)
      val updatedPerson = person.copy(location = location ,time = time, extra = extra)
      repository.save(updatedPerson)
      updatedPerson
    }
  }

  def getById(id: String): Future[Option[Person]] = repository.getById(id)

  def getByTime(start: Long, end: Option[Long]): Future[List[Person]] = repository.getByTime(start, end)

  def getByLocationAndTime(longitude: Double, latitude: Double, start: Long, end: Option[Long]): Future[List[Person]] =
    repository.getByLocationAndTime(Location(longitude, latitude), 10, start, end)
}