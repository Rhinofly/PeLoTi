package controllers

import models.MongoDB
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.Controller
import service.Service

class Application(service: Service) extends Controller {

  implicit val reads = (
    (__ \ 'latitude).read[Double] and
    (__ \ 'longitude).read[Double]) tupled
  
  def search = Action { request =>
    request.body.asJson.map(json => {
      json.validate[(Double, Double)].map {
      case (latitude, longitude) =>
        Ok(Json.obj("status" -> "OK", "people" -> service.searchPeople(latitude, longitude)))
    }.recoverTotal {
      e => BadRequest(JsError.toFlatJson(e))
    }
    }).getOrElse(BadRequest("Invalid JSON"))
  }

  def create = Action { request =>
    request.body.asJson.map(json => {
      json.validate[(Double, Double)].map {
      case (latitude, longitude) =>
        Ok(Json.obj("status" -> "OK", "id" -> service.createPerson(latitude, longitude)))
    }.recoverTotal {
      e => BadRequest(JsError.toFlatJson(e))
    }
    }).getOrElse(BadRequest("Invalid JSON"))

  }
}

object Application extends Application(new Service(MongoDB))
