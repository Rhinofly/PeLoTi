package models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports.ObjectId
import play.api.http.Status.OK
import play.api.http.Status.BAD_REQUEST


object MongoDB extends Database {
  val mongoConnection = MongoConnection()
  val mongoDB = mongoConnection("test")("test")
  
  override def search(latitude: Double, longitude: Double, radius: Long, token: String): List[Person] = {
    mongoDB.find(
        "location" $near(longitude, latitude) $maxDistance(radius * 1000), 
        MongoDBObject("token" -> token)
    ).map{ row => 
        Person(row.get("_id").toString, 
               row.as[MongoDBList]("location").toList.asInstanceOf[List[Double]],
               token)
    }.toList
  }
  
  override def create(latitude: Double, longitude: Double, token: String): String = {
    val mongoObject = MongoDBObject(
        "location" -> MongoDBList(longitude, latitude),
        "token" -> token)
    mongoDB.insert(mongoObject)
    mongoObject.get("_id").toString
  }
  
  override def update(id: String, latitude: Double, longitude: Double, token: String): Int = {
    val query = MongoDBObject("_id" -> new ObjectId(id), "token" -> token)
    val update = $set("location" -> MongoDBList(longitude, latitude))
    val result = mongoDB.update(query, update)
    if(result.getN < 1) BAD_REQUEST else OK
  }
  
  override def getPerson(id: String, token: String): Person = {
    try {
      mongoDB.findOne(MongoDBObject("_id" -> new ObjectId(id), "token" -> token)).map { row =>
        new Person(id, row.as[MongoDBList]("location").toList.asInstanceOf[List[Double]], token)
      }.getOrElse(throw new Exception("Invalid input"))
    } catch {
      case iae: IllegalArgumentException => throw new Exception("Invalid input")
    }
    
  }
}