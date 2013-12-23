package models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports.ObjectId
import play.api.http.Status.OK
import play.api.http.Status.BAD_REQUEST
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class MongoDB(databaseName: String, collection: String) extends PersonRepository {
  val mongoConnection = MongoConnection()
  val mongoCollection = mongoConnection(databaseName)(collection)
  
  override def getByLocation(location: Location, radius: Long): Future[List[Person]] = Future {
    mongoCollection.find(
        "location" $near(location.longitude, location.latitude) $maxDistance(radius * 1000)
    ).map(row => Person(row.as[MongoDBList]("location"), row.as[Long]("time"), Option(row.get("_id").toString))).toList
  }
  
  override def getByTime(time: Long): Future[List[Person]] = Future {
    mongoCollection.find(
        "time" $gte time
    ).map(row => Person(row.as[MongoDBList]("location"), row.as[Long]("time"), Option(row.get("_id").toString))).toList
  }
  
  override def getByLocationAndTime(location: Location, radius: Long, time: Long): Future[List[Person]] = Future {
    mongoCollection.find(
         ("location" $near(location.longitude, location.latitude) $maxDistance(radius * 1000)) ++
         ("time" $gte time)
    ).map(row => Person(row.as[MongoDBList]("location"), row.as[Long]("time"), Option(row.get("_id").toString))).toList
  }
  
  override def getById(id: String): Future[Person] = {
    try {
      mongoCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map { row =>
        Future { 
          new Person(row.as[MongoDBList]("location"), row.as[Long]("time"), Option(id))
        }
      }.getOrElse(throw new Exception("Invalid input"))
    } catch {
      case iae: IllegalArgumentException => throw new Exception("Invalid input")
    }
  }
  
  override def save(person: Person): Future[String] = {
    if(person.id.isDefined) {
      val id = person.id.get
      val query = MongoDBObject("_id" -> new ObjectId(id))
      val update = $set("location" -> MongoDBList(person.location.longitude, person.location.latitude), "time" -> person.time)
      val result = mongoCollection.update(query, update)
      Future(id)
    } else {
      val mongoObject = MongoDBObject(
        "location" -> MongoDBList(person.location.longitude, person.location.latitude),
        "time" -> person.time)
      mongoCollection.insert(mongoObject)
      Future(mongoObject.get("_id").toString)
    } 
  }

  implicit def toLocation(list: MongoDBList): Location = {
    new Location(list.get(0).asInstanceOf[Double], list.get(1).asInstanceOf[Double])
  }
}