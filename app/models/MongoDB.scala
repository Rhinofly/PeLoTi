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
  
  override def getByLocation(longitude: Double, latitude: Double, radius: Long): Future[List[Person]] = Future {
    mongoCollection.find(
        "location" $near(longitude, latitude) $maxDistance(radius * 1000)
    ).map{ row => 
        Person(row.as[MongoDBList]("location"), Option(row.get("_id").toString))
    }.toList
  }
  
  override def save(longitude: Double, latitude: Double, optionId: Option[String]): Future[String] = {
    if(optionId.isDefined) {
      val id = optionId.get
      val query = MongoDBObject("_id" -> new ObjectId(id))
      val update = $set("location" -> MongoDBList(longitude, latitude))
      val result = mongoCollection.update(query, update)
      Future(id)
    } else {
      val mongoObject = MongoDBObject(
        "location" -> MongoDBList(longitude, latitude)
      )
      mongoCollection.insert(mongoObject)
      Future(mongoObject.get("_id").toString)
    } 
  }
  
  override def getById(id: String): Future[Person] = {
    try {
      mongoCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map { row =>
        Future { 
          new Person(row.as[MongoDBList]("location"), Option(id))
        }
      }.getOrElse(throw new Exception("Invalid input"))
    } catch {
      case iae: IllegalArgumentException => throw new Exception("Invalid input")
    }
  }
  
  implicit def toLocation(list: MongoDBList): Location = {
    new Location(list.get(0).asInstanceOf[Double], list.get(1).asInstanceOf[Double])
  }
}