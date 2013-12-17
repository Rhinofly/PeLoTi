package models

import com.mongodb.casbah.Imports._

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
        "token" -> token
    )
    mongoDB.insert(mongoObject)
    mongoObject.get("_id").toString
  }
  
  override def update(id: String, latitude: Double, longitude: Double, token: String): String = {
    val query = MongoDBObject("_id" -> id, "token" -> token)
    val update = $set("location" -> MongoDBList(longitude, latitude))
    val result = mongoDB.update(query, update)
    result.toString
  }
}