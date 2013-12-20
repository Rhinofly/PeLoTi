package models

import com.mongodb.casbah.Imports._

trait Database {
  
  def search(latitude: Double, longitude: Double, radius: Long, token: String): List[Person]
  def create(latitude: Double, longitude: Double, token: String): String
  def update(id: String, latitude: Double, longitude: Double, token: String): Int
  def getPerson(id: String, token: String): Person
  
}