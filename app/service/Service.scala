package service

import models.Person

object Service {
  
  private def people = List(
      new Person(54.21, 5.22),
      new Person(54.23, 5.21),
      new Person(54.22, 5.21),
      new Person(54.26, 5.24)
  )
  
  def distanceInKilometer(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    val R = 6371
    val dLat = (lat2 - lat1) * Math.PI / 180
    val dLon = (lon2 - lon1) * Math.PI / 180
    var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2)
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    R * c
  }
  
  def searchPeople(latitude: Double, longitude: Double): List[Person] = {
    val radius = 10
    people.filter(person => 
      distanceInKilometer(latitude, longitude, person.Latitude, person.Longitude) < radius)
  }
}