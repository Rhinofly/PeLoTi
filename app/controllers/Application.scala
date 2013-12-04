package controllers

import models.MongoDB
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
import play.api.libs.json.Json
import scala.annotation.implicitNotFound
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.__
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import models._
import service._

class Application(service: Service) extends Controller {

  implicit val reads = (
    (__ \ 'latitude).read[Double] and
    (__ \ 'longitude).read[Double] and
    (__ \ 'token).read[String]) tupled
  
  
  def search = Action { request =>
   parseJson(request, (latitude: Double, longitude: Double, token: String) =>
      Json.obj("status" -> "OK", "people" -> service.searchPeople(latitude, longitude, token)
    )
  )}

  def create = Action { request =>
    parseJson(request, (latitude: Double, longitude: Double, token: String) =>
      Json.obj("status" -> "OK", "id" -> service.createPerson(latitude, longitude, token)
    )
  )}

  val requestForm = Form(
    "email" -> email)

  def request = Action {
    Ok(views.html.requestToken(requestForm))
  }

  def receive = Action { implicit request =>
    requestForm.bindFromRequest.fold(
        formWithErrors => BadRequest("Email is required!"),
        value => Ok(views.html.receiveToken(service.generateToken(value)))
    )
  }
  
  def parseJson(request: Request[AnyContent], matcher: (Double, Double, String) => JsValue): SimpleResult = {
    request.body.asJson.map(json => {
      json.validate[(Double, Double, String)].map {
        case (latitude, longitude, token) =>
          Ok(matcher(latitude, longitude, token))
      }.recoverTotal {
        e => BadRequest(JsError.toFlatJson(e))
      }
    }).getOrElse(BadRequest("Invalid JSON"))
  }
}

object Application extends Application(new Service(MongoDB))
