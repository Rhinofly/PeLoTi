package models

import com.mongodb.casbah.Imports._

trait Database {
  
  def search(latitude: Double, longitude: Double, radius: Long): List[Person]
  def create(latitude: Double, longitude: Double): String
  
}