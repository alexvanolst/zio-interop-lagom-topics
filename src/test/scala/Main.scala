import `macro`.KafkaClient
import sample.{SampleMessage, SampleService}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.ConsumerSettings
import zio.magic._

object Main extends zio.App {

  val kafkaClient = KafkaClient.implement[SampleService, SampleMessage](_.sampleTopic())

  val app = kafkaClient.consumeWith {
    case (k,v) => ZIO.debug(v)
  }

  override def run(args: List[String]) = app.inject(
    Blocking.live,
    Clock.live,
    ZLayer.succeed[ConsumerSettings](ConsumerSettings(List("localhost2:9092")).withGroupId("sample-group"))
  ).exitCode
}