import models.Database
import models.Person
import scala.collection.mutable.Buffer
import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import scala.language.postfixOps
import play.api.http.Status._

object DummyDatabase extends Database {

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
    val result = DB.withConnection { implicit connection => 
      SQL("""select * from person where token = {token}""").on(
          'token -> token
      ).as{
        get[Long]("person.id") ~
        get[Double]("person.latitude") ~
        get[Double]("person.longitude") ~
        get[String]("person.token") map { 
          case id ~ latitude ~ longitude ~ token => 
            Person(String.valueOf(id), List(latitude, longitude), token)
        } *
      }
    }
    result.filter(person => 
      distanceInKilometer(latitude, longitude, person.location(0), person.location(1)) < radius
    )
  }
  
  def create(latitude: Double, longitude: Double, token: String): String = {
    DB.withConnection { implicit connection => 
      val id = SQL("insert into person(latitude, longitude, token) values({latitude}, {longitude}, {token})").on(
        'latitude -> latitude,
        'longitude -> longitude,
        'token -> token).executeInsert()
      String.valueOf(id)
    }
  }
  
  
  def update(id: String, latitude: Double, longitude: Double, token: String): Int = {
    DB.withConnection { implicit connection =>
      val count = SQL("update person set latitude={latitude}, longitude={longitude} where id = {id} and token = {token}").on(
          'latitude -> latitude,
          'longitude -> longitude,
          'id -> id.toLong,
          'token -> token).executeUpdate()
      if(count != 1) BAD_REQUEST
      else OK
    }
  }
}