package models

object Config {
  def config = play.api.Play.current.configuration
  
  def databaseName = config.getString("db.default.name").getOrElse("peloti")
}