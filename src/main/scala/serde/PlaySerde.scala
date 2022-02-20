package serde

import org.apache.kafka.common.header.Headers
import play.api.libs.json.{ Format, Json }
import zio.kafka.serde.Serde
import zio.{ RIO, ZIO }

object PlaySerde {

  def serdeOf[T]()(implicit format: Format[T]): Serde[Any, T] = new Serde[Any, T] {

    override def serialize(topic: String, headers: Headers, value: T): RIO[Any, Array[Byte]] =
      ZIO.effectTotal {
        Json.toJson(value).toString().getBytes
      }

    override def deserialize(topic: String, headers: Headers, data: Array[Byte]): RIO[Any, T] =
      ZIO.effect(Json.parse(data).as[T])

  }
}
