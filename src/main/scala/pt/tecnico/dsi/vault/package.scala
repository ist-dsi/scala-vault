package pt.tecnico.dsi

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}
import io.circe.{Decoder, Encoder, Json}

package object vault {
  implicit val encodeFiniteDuration: Encoder[FiniteDuration] = Encoder[String].contramap(d => s"${d.toSeconds.toString}s")
  implicit val decoderFiniteDuration: Decoder[FiniteDuration] = Decoder[Long].emap(l => Right(FiniteDuration(l, TimeUnit.SECONDS)))

  implicit val encodeDuration: Encoder[Duration] = Encoder[String].contramap(d => if (d eq Duration.Undefined) "" else s"${d.toSeconds.toString}s")
  implicit val decoderDuration: Decoder[Duration] = Decoder[Long].emap(l => Right(Duration(l, TimeUnit.SECONDS)))

  implicit val encodeArrayAsCSV: Encoder[Array[String]] = (a: Array[String]) => Json.fromString(a.mkString(","))
  implicit val decodeArrayAsCSV: Decoder[Array[String]] = Decoder[String].map(_.split(","))
}
