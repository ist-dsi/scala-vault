package pt.tecnico.dsi

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.Sync
import io.circe.{Decoder, Encoder, Json, Printer}
import org.http4s.{EntityDecoder, EntityEncoder, Method}
import org.http4s.circe
import org.http4s.client.impl.EmptyRequestGenerator

import scala.concurrent.duration.{Duration, FiniteDuration}

package object vault {
  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf[F, A]
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf[F, A](jsonPrinter)
  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit def void[F[_]: Sync]: EntityDecoder[F, Unit] = EntityDecoder.void

  implicit val encodeFiniteDuration: Encoder[FiniteDuration] = Encoder.encodeString.contramap{ d: FiniteDuration =>
    s"${d.toSeconds.toString}s"
  }
  implicit val decoderFiniteDuration: Decoder[FiniteDuration] = Decoder.decodeLong.emap(l => Right(FiniteDuration(l, TimeUnit.SECONDS)))

  implicit val encodeDuration: Encoder[Duration] = Encoder.encodeString.contramap{ d: Duration =>
    if (d eq Duration.Undefined) "" else s"${d.toSeconds.toString}s"
  }
  implicit val decoderDuration: Decoder[Duration] = Decoder.decodeLong.emap(l => Right(Duration(l, TimeUnit.SECONDS)))

  implicit val encodeArrayAsCSV: Encoder[Array[String]] = (a: Array[String]) => Json.fromString(a.mkString(","))
  implicit val decodeArrayAsCSV: Decoder[Array[String]] = Decoder.decodeString.map(_.split(","))

  // Have you ever heard about the LIST HTTP method, neither have I, as it does not exist </faceplam>
  // Unfortunately NoBody is a sealed trait. So we must resort to a more verbose, and ugly, way of getting a nice syntax for LIST.
  // "it is not recommended to define classes/objects inside of package objects" that's why we used `with vault.type`
  // and not some marker trait. We need a marker type to ensure we are not generating an EmptyRequestGenerator for
  // methods with body.
  val LIST = Method.fromString("LIST").toOption.get.asInstanceOf[Method with vault.type]
  implicit def listOps[F[_]](list: Method with vault.type): EmptyRequestGenerator[F] = new EmptyRequestGenerator[F] {
    override def method: Method = list
  }
}
