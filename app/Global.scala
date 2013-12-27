import scala.concurrent.Future

import play.api.GlobalSettings
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.mvc.Results.NotFound

object Global extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader) =
    Future.successful(
      NotFound(Json.obj("status" -> NOT_FOUND, "message" -> s"Requested path: ${request.path}")))

}