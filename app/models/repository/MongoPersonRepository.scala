package models.repository

import com.mongodb.casbah.Imports._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models._

class MongoPersonRepository(databaseName: String, collection: String) extends PersonRepository {
  val mongoConnection = MongoConnection()
  val mongoCollection = mongoConnection(databaseName)(collection)
  
  override def getByLocation(location: Location, radius: Long): Future[List[Person]] = Future {
    val search = MongoDBObject("location" -> MongoDBObject(
        "$near" -> MongoDBObject(
            "$geometry" -> MongoDBObject(
                "type" -> "Point",
                "coordinates" -> GeoCoords(location.longitude, location.latitude)),
            "$maxDistance" -> radius)))
    mongoCollection.find(search
    ).map{ row => 
      Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString))
    }.toList
  }
  
  override def getByTime(start: Long, optionEnd: Option[Long]): Future[List[Person]] = Future {
    optionEnd.map(end => mongoCollection.find("time" $gte start $lte end)
    ).getOrElse(mongoCollection.find("time" $gte start))
    .map(row => Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString))).toList
  }
  
  override def getByLocationAndTime(location: Location, radius: Long, start: Long, optionEnd: Option[Long]): Future[List[Person]] = Future {
    val search = MongoDBObject("location" -> MongoDBObject(
        "$near" -> MongoDBObject(
            "$geometry" -> MongoDBObject(
                "type" -> "Point",
                "coordinates" -> GeoCoords(location.longitude, location.latitude)),
            "$maxDistance" -> radius)))
    optionEnd.map(end => mongoCollection.find(search ++ ("time" $gte start $lte end))
    ).getOrElse(mongoCollection.find(search ++ ("time" $gte start))
    ).map(row => Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString))).toList
  }
  
  override def getById(id: String): Future[Person] = {
    try {
      mongoCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map { row =>
        Future { 
          new Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(id))
        }
      }.getOrElse(throw new Exception(s"No person found with id $id"))
    } catch {
      case iae: IllegalArgumentException => throw new Exception("Invalid id")
    }
  }
  
  override def save(person: Person): Future[String] = {
    if(person.id.isDefined) {
      val id = person.id.get
      val query = MongoDBObject("_id" -> new ObjectId(id))
      val update = $set(
          "location" -> MongoDBObject("type" -> "Point", "coordinates" -> GeoCoords(person.location.longitude, person.location.latitude)), 
          "time" -> person.time)
      val result = mongoCollection.update(query, update)
      Future(id)
    } else {
      val mongoObject = MongoDBObject(
        "location" -> MongoDBObject("type" -> "Point", "coordinates" -> GeoCoords(person.location.longitude, person.location.latitude)),
        "time" -> person.time)
      mongoCollection.insert(mongoObject)
      Future(mongoObject.get("_id").toString)
    } 
  }

  implicit def toLocation(dbo: DBObject): Location = {
    val list = dbo.as[MongoDBList]("coordinates")
    new Location(list.get(0).asInstanceOf[Double], list.get(1).asInstanceOf[Double])
  }
}