package service

import models.Person
import models.Database
import play.api.libs.Codecs
import scala.collection.immutable.StringOps

class Service(database: Database) {
  
  def searchPeople(latitude: Double, longitude: Double, token: String): List[Person] = {
    database.search(latitude, longitude, 10, token)
  }
  
  def createPerson(latitude: Double, longitude: Double, token: String): String = {
    database.create(latitude, longitude, token)
  }
  
  def generateToken(email: String): String = {
    Codecs.md5(email.getBytes)
  }
}