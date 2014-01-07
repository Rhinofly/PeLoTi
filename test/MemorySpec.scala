package test

import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.{ global => ExecutionGlobal }
import org.specs2.execute.AsResult
import org.specs2.mutable.Specification
import org.specs2.specification._
import org.specs2.time.NoTimeConversions
import models.service._
import models.repository._
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.test._
import play.api.test.Helpers._
import reactor.Portal
import test.repository.MemoryUserRepository
import test.repository.MemoryPersonRepository
import global.Global

class MemorySpec extends Specification with DatabaseTests with NoTimeConversions {

  "MemoryPersonRepository" should personRepositoryTests
  
  "MemoryUserRepository" should userRepositoryTests 
  
  lazy val personRepository = new MemoryPersonRepository
  lazy val userRepository = new MemoryUserRepository
}