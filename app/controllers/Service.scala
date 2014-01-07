package controllers

import scala.concurrent.Future

import models.RequestHandler
import models.requests.Create
import models.requests.Update
import models.service.Person
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.SimpleResult
import reactor.{Service => ServiceReaktor}

class Service(service: ServiceReaktor) extends Controller with RequestHandler {

  def byLocation(longitude: Double, latitude: Double, radius: Long) = Action.async { implicit request =>
    tryWithRecover {
      service.getByLocation(longitude, latitude, radius).map(list => Ok(response(OK, "people" -> list)))
    }
  }

  def create = Action.async { implicit request =>
    tryWithRecover {
      validateForm(Create.form, service.createPerson)
    }
  }

  def update = Action.async { implicit request =>
    tryWithRecover {
      validateForm(Update.form, service.updatePerson)
    }
  }

  def byId(id: String) = Action.async { implicit request =>
    tryWithRecover {
      service.getById(id).map(person => Ok(response(OK, "person" -> person)))
        .recover {
          case _: IllegalArgumentException | _: NoSuchElementException =>
            BadRequest(error(s"Invalid id: $id", BAD_REQUEST))
        }
    }
  }

  def getByTime(start: Long, end: Option[Long]) = Action.async { implicit request =>
    tryWithRecover {
      service.getByTime(start, end).map(list => Ok(response(OK, "people" -> list)))
    }
  }

  def getByLocationAndTime(longitude: Double, latitude: Double, start: Long, end: Option[Long]) = Action.async { implicit request =>
    tryWithRecover {
      service.getByLocationAndTime(longitude, latitude, start, end).map(list => Ok(response(OK, "people" -> list)))
    }
  }

  private def validateForm[T](form: Form[T], function: T => Future[Person])(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    form.bindFromRequest.fold(
      formWithErrors => formErrors(formWithErrors),
      request => function(request).map(person => Ok(response(OK, "person" -> person))))
  }
}