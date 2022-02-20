package `macro`

import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.broker.Topic
import zio.{ZIO, _}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer._

import scala.language.experimental.macros

trait KafkaClient[+T] {
  def consumeWith[R](
    zio: (String, T) => URIO[R, Unit]
  ): ZIO[R with Blocking with Clock with Has[ConsumerSettings], Throwable, Unit]
}

object KafkaClient {

  def implement[S <: Service, T](topicCall: S => Topic[T]): KafkaClient[T] = macro KafkaClientMacroImpl.implementClient[S,T]
}
