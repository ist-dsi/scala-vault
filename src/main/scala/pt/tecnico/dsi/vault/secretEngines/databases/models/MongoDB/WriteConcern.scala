package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.syntax._
import pt.tecnico.dsi.vault.decoderFiniteDuration

object WriteConcern {
  val firstEncoder: Encoder.AsObject[WriteConcern] =
    Encoder.forProduct3("w", "j", "wtimeout")(w => (w.writeMode, w.onDiskAcknowledge, w.timeout.toMillis))
  val lastDecoder: Decoder[WriteConcern] =
    Decoder.forProduct3("w", "j", "wtimeout")(WriteConcern.apply)

  implicit val encoder: Encoder[WriteConcern] = Encoder.encodeString.contramap(_.asJson(firstEncoder).noSpaces)
  implicit val decoder: Decoder[WriteConcern] = Decoder.decodeString.emap(decode(_)(lastDecoder).left.map(_.getMessage))
}
// The go package Vault uses https://godoc.org/gopkg.in/mgo.v2#Safe does not accept a String writeConcern
case class WriteConcern(writeMode: Int = 1, onDiskAcknowledge: Boolean, timeout: FiniteDuration = 5000.millis)
