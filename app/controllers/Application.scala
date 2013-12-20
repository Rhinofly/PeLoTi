package controllers

import models.MongoDB
import models.requests.RequestHandler._
import play.api.libs.json._
import play.api.mvc._
import service.Service

class Application(service: Service) extends Controller {

  def search = Action { request =>
    val action = (service.searchPeople _)
    parseJson(request, action)
  }

  def create = Action { request =>
    val action = (service.createPerson _)
    parseJson(request, action)
  }

  def update = Action { request =>
    val action = (service.updatePerson _)
    parseJson(request, action)
  }
  
  def get = Action { request =>
    val action = (service.getPerson _)
    parseJson(request, action)
  }

  def parseJson[T](request: Request[AnyContent], action: T => JsValue)(implicit reads: Reads[T]): SimpleResult = {
    request.body.asJson.map(json => {
      json.validate(reads).map { result =>
        Ok(action(result))
      }.recoverTotal {
        e => BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> JsError.toFlatJson(e)))
      }
    }).getOrElse(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid JSON")))
  }
}

object Application extends Application(Service(MongoDB))
