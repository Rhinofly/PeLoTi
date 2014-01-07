package models.requests

trait AsMap {
	def asMap: Map[String, Seq[String]]
}