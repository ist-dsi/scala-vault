package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.decoderFiniteDuration

case class WriteConcern(writeMode: String = "majority", onDiskAcknowledge: Boolean, timeout: FiniteDuration = 5000.millis)

object WriteConcern {
  val firstEncoder: Encoder[WriteConcern] =
    Encoder.forProduct3("w", "j", "wtimeout")(w => (w.writeMode, w.onDiskAcknowledge, w.timeout.toMillis))
  val lastDecoder: Decoder[WriteConcern] =
    Decoder.forProduct3("w", "j", "wtimeout")(WriteConcern.apply)

  implicit val encoder: Encoder[WriteConcern] = Encoder.encodeString.contramap(_.asJson(firstEncoder).noSpaces)
  implicit val decoder: Decoder[WriteConcern] = Decoder.decodeString.emap(decode(_)(lastDecoder).left.map(_.getMessage))
}
