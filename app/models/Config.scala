package models

import play.api.Application

object Config {
  def databaseName(implicit app:Application) = app.configuration.getString("db.default.name").getOrElse("peloti")
  def apiUrl(implicit app:Application) = app.configuration.getString("peloti.url").getOrElse("localhost:9000")
}