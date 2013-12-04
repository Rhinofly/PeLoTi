import models.Database
import models.Person

object DummyDatabase extends Database {

  private var persons = List(
      new Person("0", List(5.22, 54.21)),
      new Person("1", List(5.21, 54.23)),
      new Person("2", List(5.21, 54.22)),
      new Person("3", List(5.24, 54.26))
  )
  
  private def distanceInKilometer(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    val R = 6371
    val dLat = (lat2 - lat1) * Math.PI / 180
    val dLon = (lon2 - lon1) * Math.PI / 180
    var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2)
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    R * c
  }
  
  def search(latitude: Double, longitude: Double, radius: Long): List[Person] = {
    persons.filter(person => 
      distanceInKilometer(latitude, longitude, person.location(0), person.location(1)) < radius)
  }
  
  def create(latitude: Double, longitude: Double): String = {
    val person = new Person(persons.length.toString, List(latitude, longitude)) 
    persons ::= person
    person.id
  }
  
}