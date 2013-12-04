import models.Database
import models.Person

object DummyDatabase extends Database {

  private var persons = List(
      new Person("0", List(5.22, 54.21), "string1"),
      new Person("1", List(5.21, 54.23), "string1"),
      new Person("2", List(5.21, 54.22), "string1"),
      new Person("3", List(5.24, 54.26), "string1"),
      new Person("0", List(5.22, 54.21), "string2"),
      new Person("1", List(5.21, 54.23), "string2"),
      new Person("2", List(5.21, 54.22), "string2"),
      new Person("3", List(5.24, 54.26), "string2")
  )
  
  private def d(point1: Double, point2: Double) = (point1 - point2) * Math.PI / 180
  private def sin(point: Double) = Math.sin(point / 2)
  private def multiplySin(point1: Double, point2: Double) = sin(point1) * sin(point2)
  private def cos(point: Double) = Math.cos(point * Math.PI / 180)
  private def multiplyCos(point1: Double, point2: Double) = cos(point1) * cos(point2)
  
  private def distanceInKilometer(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    val R = 6371
    val dLat = d(lat2, lat1)
    val dLon = d(lon2, lon1)
    var a = multiplySin(dLat, dLat) + multiplyCos(lat1, lat2) * multiplySin(dLon, dLon)
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    R * c
  }
  
  def search(latitude: Double, longitude: Double, radius: Long, token: String): List[Person] = {
    persons.filter(person => (
      person.token == token &&  
      distanceInKilometer(latitude, longitude, person.location(0), person.location(1)) < radius))
      
  }
  
  def create(latitude: Double, longitude: Double, token: String): String = {
    val person = new Person(persons.length.toString, List(latitude, longitude), token) 
    persons ::= person
    person.id
  }
  
}