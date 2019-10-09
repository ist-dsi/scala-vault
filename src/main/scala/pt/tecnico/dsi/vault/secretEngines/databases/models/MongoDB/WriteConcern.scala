package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import pt.tecnico.dsi.vault.{decoderFiniteDuration, encodeFiniteDuration}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class WriteConcern(writeMode: String = "majority", onDiskAcknowledge: Boolean, timeout: FiniteDuration = 5000.millis)

object WriteConcern {
  private val firstEncoder: Encoder[WriteConcern] =
    Encoder.forProduct3("w", "j", "wtimeout")(w => (w.writeMode, w.onDiskAcknowledge, w.timeout))
  private val lastDecoder: Decoder[WriteConcern] =
    Decoder.forProduct3("w", "j", "wtimeout")(WriteConcern.apply)


  implicit val encoder: Encoder[WriteConcern] = Encoder.encodeString.contramap(w => w.asJson(firstEncoder).noSpaces)
  implicit val decoder: Decoder[WriteConcern] = new Decoder[WriteConcern] {
    override def apply(c: HCursor): Result[WriteConcern] = Decoder.decodeString(c).flatMap(s => lastDecoder.decodeJson(Json.fromString(s)))
  }
}
