package pt.tecnico.dsi.vault.models

import io.circe.{Decoder, Encoder}

object TokenType {
  implicit val encoder: Encoder[TokenType] =
    Encoder.encodeString.contramap(_.toString.toLowerCase.replace("default", "default-"))
  implicit val decoder: Decoder[TokenType] = Decoder.decodeString.emap {
    case "service" => Right(Service)
    case "batch" => Right(Batch)
    case "default-service" => Right(DefaultService)
    case "default-batch" => Right(DefaultBatch)
    case t => Left(s"Unknown token type $t")
  }
}
sealed trait TokenType
case object Service extends TokenType
case object Batch extends TokenType
case object DefaultService extends TokenType
case object DefaultBatch extends TokenType