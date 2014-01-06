package models

case class ExtraField(key: String, value: String)
case class ExtraDataRequest(id: String, values: List[ExtraField])