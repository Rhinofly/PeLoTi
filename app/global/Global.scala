package global

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import controllers.Portal
import controllers.PortalAPI
import controllers.Service
import models.BlowfishEncryption
import models.Config
import models.Encryption
import models.repository.MongoPersonRepository
import models.repository.MongoUserRepository
import models.repository.PersonRepository
import models.repository.UserRepository
import play.api.Application
import play.api.GlobalSettings
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.RequestHeader
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactor.{Portal => PortalReactor}
import reactor.{Service => ServiceReactor}

class Global(val personRepositoryFactory: Application => Future[PersonRepository],
  			 val userRepositoryFactory: Application => UserRepository,
  			 val encryption: Encryption) extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] =
    Future.successful(
      NotFound(Json.obj("status" -> NOT_FOUND, "message" -> s"Requested path: ${request.path}")))

  override def onBadRequest(request: RequestHeader, error: String): Future[SimpleResult] = {
    Future.successful(
      BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> error)))
  }

  var service: ServiceReactor = _
  var portal: PortalReactor = _
  var address: String = _

  override def onStart(app: Application) = {
    // During startup application does not require to be reactive, this is the best place to wait for database indexes to be made 
    val personRepository = Await.result(personRepositoryFactory(app), 5.seconds)
    service = new ServiceReactor(personRepository)
    portal = new PortalReactor(userRepositoryFactory(app), encryption)
    address = Config.apiUrl(app)
  }

  lazy val controllers: Map[Class[_], _] = Map(
    classOf[Service] -> new Service(service),
    classOf[Portal] -> new Portal(address),
    classOf[PortalAPI] -> new PortalAPI(portal))

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    controllers.get(controllerClass).getOrElse(sys.error(s"Could not find controller for class with name '${controllerClass.getName}'")).asInstanceOf[A]
  }
}

object Global extends Global({
  implicit application => MongoPersonRepository(ReactiveMongoPlugin.db, "test")
}, {
  implicit application => new MongoUserRepository(ReactiveMongoPlugin.db, "test")
}, new BlowfishEncryption)
