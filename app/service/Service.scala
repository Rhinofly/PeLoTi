package service

import models.Person
import models.Database

class Service(database: Database) {
  
  def searchPeople(latitude: Double, longitude: Double): List[Person] = {
    database.search(latitude, longitude, 10)
  }
  
  def createPerson(latitude: Double, longitude: Double): String = {
    database.create(latitude, longitude)
  }
}