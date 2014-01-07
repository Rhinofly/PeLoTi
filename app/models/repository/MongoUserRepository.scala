package models.repository

import scala.concurrent.Future
import scala.language.postfixOps

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.DefaultDB
import reactivemongo.core.commands.LastError

// This class contains all functionality for storing a users into MongoDB
class MongoUserRepository(database: DefaultDB, collectionName: String) extends UserRepository {
  def collection = database.collection[JSONCollection](collectionName)

  val time = 1000 * 60 * 10

  override def create(email: String, password: String, token: String): Future[Unit] = {
    val create = Json.obj(
      "_id" -> email,
      "password" -> password,
      "token" -> token)
    collection.insert(create).map(result => if(!result.ok) throw result)
    .recover {
      case le : LastError => throw new Exception(le.getMessage)
    }
  }

  override def getToken(email: String): Future[Option[String]] = getSingle(email).map {
    case Some(json) => (json \ "token").asOpt[String]
    case None => None
  }

  override def exists(email: String): Future[Boolean] = getSingle(email).map {
    case Some(json) => true
    case None => false
  }

  override def getPasswordHash(email: String): Future[Option[String]] = getSingle(email).map {
    case Some(json) => (json \ "password").asOpt[String]
    case None => None
  }

  override def changePassword(email: String, password: String): Future[Unit] = Future {
    val search = Json.obj("_id" -> email)
    val update = Json.obj("$set" -> Json.obj("password" -> password))
    collection.update(search, update)
  }

  override def setResetPassword(email: String, token: String): Future[Unit] = Future {
    val search = Json.obj("_id" -> email)
    val update = Json.obj("$set" -> Json.obj("resetToken" -> token, "resetTime" -> (System.currentTimeMillis() + time)))
    collection.update(search, update)
  }

  override def resetPassword(password: String, token: String): Future[Boolean] = {
    val search = Json.obj("resetToken" -> token, "resetTime" -> Json.obj("$gte" -> System.currentTimeMillis))
    val update = Json.obj("$set" -> Json.obj("password" -> password), "$unset" -> Json.obj("resetToken" -> "", "resetTime" -> ""))
    Future.successful(collection.update(search, update).isCompleted)
  }

  def getSingle(email: String): Future[Option[JsObject]] =
    collection.find(Json.obj("_id" -> email)).one[JsObject]
}