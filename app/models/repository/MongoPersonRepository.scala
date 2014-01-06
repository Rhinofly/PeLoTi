package models.repository

import com.mongodb.casbah.Imports._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models._
import scala.language.implicitConversions
import scala.collection.JavaConversions._

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
      Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString),getExtra(row.getAs[DBObject]("extra")))
    }.toList
  }
  
  override def getByTime(start: Long, optionEnd: Option[Long]): Future[List[Person]] = Future {
    optionEnd.map(end => mongoCollection.find("time" $gte start $lte end)
    ).getOrElse(mongoCollection.find("time" $gte start))
    .map(row => Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString), getExtra(row.getAs[DBObject]("extra")))).toList
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
    ).map(row => Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(row.get("_id").toString), getExtra(row.getAs[DBObject]("extra")))).toList
  }
  
  override def getById(id: String): Future[Person] = {
    try {
      mongoCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map { row =>
        Future { 
          new Person(row.as[DBObject]("location"), row.as[Long]("time"), Option(id), getExtra(row.getAs[DBObject]("extra")))
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
  
  override def saveExtra(id: String, list: List[(String, String)]): Future[Person] = {
    val query = MongoDBObject("_id" -> new ObjectId(id))
    val update = $set("extra" -> MongoDBObject(list :_*))
    mongoCollection.update(query, update)
    getById(id)
  }

  implicit def toLocation(dbo: DBObject): Location = {
    val list = dbo.as[MongoDBList]("coordinates")
    new Location(list.get(0).asInstanceOf[Double], list.get(1).asInstanceOf[Double])
  }
  
  def getExtra(optionDbo: Option[DBObject]): Option[Map[String, String]] = {
    optionDbo.map { dbo => 
      val list = dbo.keySet.map(key => key -> dbo.getAs[String](key).getOrElse(throw new Exception("Key Failure")))
      Option(Map(list.toSeq: _*))
    }.getOrElse(None)
  }
}