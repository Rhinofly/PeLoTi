package models.repository

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.language.postfixOps

import models.service.Location
import models.service.Person
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.__
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

// This class contains all functionality for storing a person into MongoDB
class MongoPersonRepository private (collection: JSONCollection) extends PersonRepository {

  //TODO Put in configuration => Default timespan
  val defaultTimespan = 1000 * 60 * 10

  implicit val format = MongoPersonRepository.mongoFormat(Person.format)

  private def generateId: String = BSONObjectID.generate.stringify

  override def getByLocation(location: Location, radius: Long): Future[List[Person]] = {
    val search = byLocationSelector(location, radius)
    collection.find(search).cursor[Person].collect[List]()
  }

  override def getByTime(start: Long, optionEnd: Option[Long]): Future[List[Person]] = {
    val search = optionEnd.map(end => byTimeSelector(start, end))
      .getOrElse(byTimeSelector(start, start + defaultTimespan))
    collection.find(search).cursor[Person].collect[List]()
  }

  override def getByLocationAndTime(location: Location, radius: Long, start: Long, optionEnd: Option[Long]): Future[List[Person]] = {
    val search = byLocationSelector(location, radius) ++
      optionEnd.map(end => byLocationSelector(location, radius) ++ byTimeSelector(start, end))
      .getOrElse(byTimeSelector(start, start + defaultTimespan))
    collection.find(search).cursor[Person].collect[List]()
  }

  override def getById(id: String): Future[Option[Person]] =
    collection.find(Json.obj("_id" -> Json.obj("$oid" -> id))).one[Person]

  override def save(person: Person): Future[Person] = {
    val id = person.id.getOrElse(generateId)
    val toSave = person.copy(id = Some(id))
    collection.save(toSave)
    Future.successful(toSave)
  }

  private def byLocationSelector(location: Location, radius: Long) =
    Json.obj("location" -> Json.obj(
      "$near" -> Json.obj(
        "$geometry" -> Json.obj(
          "type" -> "Point",
          "coordinates" -> List(location.longitude, location.latitude)),
        "$maxDistance" -> radius * 1000)))

  private def byTimeSelector(start: Long, end: Long) =
    Json.obj("time" -> Json.obj("$gte" -> start, "$lte" -> end))
}

object MongoPersonRepository {
  def apply(database: DefaultDB, collectionName: String) = {
    val collection = database.collection[JSONCollection](collectionName)
    val indexCreated = collection.indexesManager.ensure(new Index(Seq("location" -> IndexType.Geo2DSpherical)))
    indexCreated.map(_ => new MongoPersonRepository(collection))
  }

  def mongoFormat(baseFormat: Format[Person]): Format[Person] = {
    val transformIdFromMongo = 
      __.json.update((__ \ 'id).json.copyFrom((__ \ '_id \ '$oid).json.pick))

    val transformIdToMongo = 
      __.json.update((__ \ '_id \ '$oid).json.copyFrom((__ \ 'id).json.pick))

    val mongoReads = transformIdFromMongo andThen baseFormat
    val mongoWrites = baseFormat transform (json => json.transform(transformIdToMongo).get)
    Format(mongoReads, mongoWrites)
  }
}