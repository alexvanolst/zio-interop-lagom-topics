package `macro`

import `macro`.KafkaClientMacroImpl.ExtractedMethods
import com.lightbend.lagom.scaladsl.api.ServiceSupport.ScalaMethodTopic
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import com.lightbend.lagom.scaladsl.api.broker.Topic

import scala.reflect.macros.blackbox

private object KafkaClientMacroImpl {
  final case class ExtractedMethods[MethodSymbol](serviceCalls: Seq[MethodSymbol], topics: Seq[MethodSymbol])
}

private class KafkaClientMacroImpl(val c: blackbox.Context) {
  import c.universe._
  private def abort(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

  // Copied from https://github.com/lagom/lagom/blob/main/service/scaladsl/client/src/main/scala/com/lightbend/lagom/internal/scaladsl/client/ScaladslClientMacroImpl.scala
  // Don't see a reason to replace, make sure when (or if) publishing this that we comply with Apache license 2.0
  def validateServiceInterface[T <: Service](implicit serviceType: WeakTypeTag[T]): ExtractedMethods[MethodSymbol] = {
    val serviceCallType = c.mirror.typeOf[ServiceCall[_, _]].erasure
    val topicType       = c.mirror.typeOf[Topic[_]].erasure

    val serviceMethods = serviceType.tpe.members.collect {
      case method if method.isAbstract && method.isMethod => method.asMethod
    }

    val serviceCallMethods = serviceMethods.collect {
      case serviceCall if serviceCall.returnType.erasure =:= serviceCallType => serviceCall
    }

    val topicMethods = serviceMethods.collect {
      case topic if topic.returnType.erasure =:= topicType => topic
    }

    // Check that descriptor is not abstract
    if (serviceMethods.exists(m => m.name.decodedName.toString == "descriptor" && m.paramLists.isEmpty)) {
      abort(s"${serviceType.tpe}.descriptor must be implemented in order to generate a Lagom client.")
    }

    // Make sure there are no overloaded abstract methods. This limitation is due to us only looking up service calls
    // by method name, and could be removed in the future.
    val duplicates = serviceMethods.groupBy(_.name.decodedName.toString).mapValues(_.toSeq).filter(_._2.size > 1)
    if (duplicates.nonEmpty) {
      abort(
        "Overloaded service methods are not allowed on a Lagom client, overloaded methods are: " + duplicates.keys
          .mkString(", ")
      )
    }

    // Validate that all the abstract methods are service call methods or topic methods
    val nonServiceCallOrTopicMethods = serviceMethods.toSet -- serviceCallMethods -- topicMethods
    if (nonServiceCallOrTopicMethods.nonEmpty) {
      abort(
        s"Can't generate a Lagom client for ${serviceType.tpe} since the following abstract methods don't return service calls or topics:${nonServiceCallOrTopicMethods
          .map(_.name)
          .mkString("\n", "\n", "")}"
      )
    }

    // Validate that all topics have zero parameters
    topicMethods.foreach { topic =>
      if (topic.paramLists.flatten.nonEmpty) {
        abort(s"Topic methods must have zero parameters")
      }
    }

    ExtractedMethods(serviceCallMethods.toSeq, topicMethods.toSeq)
  }

  // Copied from https://github.com/lagom/lagom/blob/main/service/scaladsl/server/src/main/scala/com/lightbend/lagom/internal/scaladsl/server/ScaladslServerMacroImpl.scala
  def readDescriptor[S <: Service](implicit serviceType: WeakTypeTag[S]): Expr[Descriptor] = {
    val extracted = validateServiceInterface[S](serviceType)

    val serviceMethodImpls: Seq[Tree] = (extracted.serviceCalls ++ extracted.topics).map { serviceMethod =>
      val methodParams = serviceMethod.paramLists.map { paramList =>
        paramList.map(param => q"${param.name.toTermName}: ${param.typeSignature}")
      }

      q"""
        override def ${serviceMethod.name}(...$methodParams) = {
          throw new _root_.scala.NotImplementedError("Service methods and topics must not be invoked from service trait")
        }
      """
    } match {
      case Seq() => Seq(EmptyTree)
      case s     => s
    }

    c.Expr[Descriptor](q"""
      new ${serviceType.tpe} {
        ..$serviceMethodImpls
      }.descriptor
    """)
  }

  // TODO: This could be reworked to have better error messages
  def getTopicFunctionName(tree: Function, candidates: Seq[MethodSymbol]): Name =
    tree.children(1).asInstanceOf[Apply].fun.asInstanceOf[Select].name

  def getTopicName[S <: Service](topicName: Name)(implicit serviceType: WeakTypeTag[S]) = {

    val descriptor = c.eval(readDescriptor)

    val functionToKafkaTopics = descriptor.topics.map { topicCall =>
      topicCall.topicHolder
        .asInstanceOf[ScalaMethodTopic[_]] // Need this cast because lagom hides internals behind unsealed trait
        .method
        .getName -> topicCall.topicId.name
    }

    functionToKafkaTopics.find(
      _._1 == topicName.decodedName.toString
    )
  }

  def implementClient[S <: Service, T](
    topicCall: Expr[S => Topic[T]]
  )(implicit serviceType: WeakTypeTag[S], topicType: WeakTypeTag[T]): Expr[KafkaClient[T]] = {
    val topicFunctionName =
      getTopicFunctionName(topicCall.tree.asInstanceOf[Function], validateServiceInterface[S].topics)

    getTopicName(topicFunctionName) match {
      case Some((function, topicName)) =>
        val quotedStrName = q"$topicName"
        c.Expr[KafkaClient[T]](
          q"""
          new KafkaClient[${topicType.tpe}] {

            import zio._
            import zio.blocking.Blocking
            import zio.clock.Clock
            import zio.kafka.consumer.{ Consumer, ConsumerSettings, Subscription }
            import zio.kafka.serde.Serde
            import serde.PlaySerde

            private val subscription = zio.kafka.consumer.Subscription.topics($quotedStrName)

            override def consumeWith[R](zio: (String, ${topicType.tpe}) => URIO[R, Unit]): ZIO[R with Blocking with Clock with Has[ConsumerSettings], Throwable, Unit] =
              for {
                 consumerSettings <- ZIO.service[ConsumerSettings]
                 _ <- Consumer.consumeWith(consumerSettings, subscription, Serde.string, PlaySerde.serdeOf[SampleMessage]) {
                        case (k, v) => zio(k, v)
                }
              } yield ()
      }
      """
        )
      case None => c.abort(c.enclosingPosition, s"Can't find matching descriptor for ${topicCall}")
    }
  }
}
