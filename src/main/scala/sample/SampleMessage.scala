package sample

import play.api.libs.json.Json

case class SampleMessage(messageContent: String)

object SampleMessage {
  implicit val format = Json.format[SampleMessage]
}
