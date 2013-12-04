package models

import com.mongodb.casbah.Imports._

object MongoDB extends Database {
  val mongoConnection = MongoConnection()
  val column = mongoConnection("test")("test")
  
  override def search(latitude: Double, longitude: Double, radius: Long, token: String): List[Person] = {
    val result = column.find(
        "location" $near(longitude, latitude) $maxDistance(radius * 1000), 
        MongoDBObject("token" -> token))
    var list: List[Person] = List()
    for(p <- result) {
      list ::= Person(p.get("_id").toString, 
                      p.as[MongoDBList]("location").toList.asInstanceOf[List[Double]],
                      token)
    }
    list
  }
  
  override def create(latitude: Double, longitude: Double, token: String): String = {
    val mongoObject = MongoDBObject(
        "location" -> MongoDBList(longitude, latitude),
        "token" -> token
    )
    column.insert(mongoObject)
    mongoObject.get("_id").toString
  }
}