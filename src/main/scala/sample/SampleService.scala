package sample

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service }

trait SampleService extends Service {

  final val TOPIC_NAME   = "sample_topic"
  final val TOPIC_NAME_2 = "sample_topic_2"

  def sampleTopic(): Topic[SampleMessage]
  def sampleTopic2(): Topic[SampleMessage]

  final override def descriptor: Descriptor = {
    import Service._

    named("sample-service").withTopics(
      topic(TOPIC_NAME, sampleTopic()),
      topic(TOPIC_NAME_2, sampleTopic2())
    )
  }
}
