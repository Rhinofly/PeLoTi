package test

import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import org.specs2.execute.AsResult
import org.specs2.mutable.Specification
import org.specs2.specification.AroundExample
import org.specs2.specification.Fragments
import org.specs2.specification.Step
import org.specs2.time.NoTimeConversions

import models.repository.MongoPersonRepository
import models.repository.MongoUserRepository
import models.service.Location
import models.service.Person
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver
import reactivemongo.api.indexes.IndexType

class MongoDBSpec extends Specification with DatabaseTests with NoTimeConversions with AroundExample {

  "MongoPersonRepository" should {

    "create an index if it doesn't exist" in {
      connection.collection[JSONCollection]("testLocationIndex").drop
      val repository = MongoPersonRepository(connection, "testLocationIndex")
      repository.map { _ =>
        var list = Await.result(connection.collection[JSONCollection]("testLocationIndex").indexesManager.list, 5 seconds)
        list.filter(index => index.key == Seq("location" -> IndexType.Geo2DSpherical)).size == 1
      } must beEqualTo(true).await
    }
    personRepositoryTests
  }

  "MongoUserRepository" should userRepositoryTests

  private def person = new Person(new Location(54.2, 5.2), 156643413L, None)

  private val personData = List(
    (Location(54.2, 5.2), 156643413L),
    (Location(54.3, 5.2), 156643413L),
    (Location(54.2, 5.2), 156643417L),
    (Location(54.2, 5.3), 156643413L))

  private def databaseStartup {
    if (checkServer) {
      val collection = connection.collection[JSONCollection]("test")
      Await.ready(collection.drop, 5 seconds)
      for (i <- 1 to 4)
        personData(i - 1) match {
          case (location, time) => personRepository.save(new Person(location, time, None))
        }
    }
  }

  private def checkServer: Boolean = {
    val socket = new Socket()
    try {
      val address = new InetSocketAddress("localhost", MongoConnection.DefaultPort)
      socket.connect(address)
      socket.isConnected
    } catch {
      case e: ConnectException => false
    } finally {
      socket.close()
    }
  }

  private val databaseName = "test"
  private val testMongo = true
  lazy val personRepository = Await.result(MongoPersonRepository(connection, "test"), 5 seconds)
  lazy val userRepository = new MongoUserRepository(connection, "test")

  val connection = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    connection("test")
  }

  override def around[T: AsResult](t: => T) = {
    if (checkServer) AsResult(t)
    else if (testMongo) failure("MongoDB is not available")
    else skipped("MongoDB is not available")
  }

  override def map(fs: => Fragments) = Step(databaseStartup) ^ fs
}