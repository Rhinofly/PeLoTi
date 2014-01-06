package models.repository

import com.mongodb.casbah.Imports._
import scala.concurrent.Await
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.language.postfixOps 

class MongoUserRepository(databaseName: String, collection: String) extends UserRepository {
  val mongoConnection = MongoConnection()
  val mongoCollection = mongoConnection(databaseName)(collection)
  
  override def create(email: String, password: String, token: String) {
    val mongoObject = MongoDBObject(
        "_id" -> email,
        "password" -> password,
        "token" -> token)
    mongoCollection.insert(mongoObject, WriteConcern.Safe)
  }
  
  override def getToken(email: String): Option[String] = {
    val result = mongoCollection.findOneByID(email)
    result.map(r => r.as[String]("token"))
  }
  
  override def exists(email: String): Boolean = {
    val result = Future(mongoCollection.findOneByID(email).map(_ => true).getOrElse(false))
    Await.result(result, 3 seconds)
  }
  
  override def getPassword(email: String): Option[String] = {
    mongoCollection.findOneByID(email).map(user => {
      user.as[String]("password")
    })
  }
  
  override def changePassword(email: String, newPassword: String) {
      val query = MongoDBObject("_id" -> email)
      val update = $set(
          "password" -> newPassword)
      val result = mongoCollection.update(query, update)
  }
}