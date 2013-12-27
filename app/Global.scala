import scala.concurrent.Future
import play.api.GlobalSettings
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.mvc.SimpleResult

object Global extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] =
    Future.successful(
      NotFound(Json.obj("status" -> NOT_FOUND, "message" -> s"Requested path: ${request.path}")))
  
  override def onBadRequest(request: RequestHeader, error: String): Future[SimpleResult] = {
    Future.successful(
      BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> error))
    )
  }

}