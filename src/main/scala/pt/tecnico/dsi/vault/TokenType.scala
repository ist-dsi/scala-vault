package pt.tecnico.dsi.vault

import enumeratum.*
import io.circe.{Decoder, Encoder}

sealed trait TokenType extends EnumEntry

case object TokenType extends Enum[TokenType] {
  // A little ugly :(
  implicit val encoder: Encoder[TokenType] = {
    case Default => Encoder.encodeString("default")
    case Service => Encoder.encodeString("service")
    case Batch => Encoder.encodeString("batch")
    case DefaultService => Encoder.encodeString("default-service")
    case DefaultBatch => Encoder.encodeString("default-batch")
  }
  implicit val decoder: Decoder[TokenType] = Decoder.decodeString.emap {
    case "default" => Right(Default)
    case "service" => Right(Service)
    case "batch" => Right(Batch)
    case "default-service" => Right(DefaultService)
    case "default-batch" => Right(DefaultBatch)
    case other => Left(s"'$other' is not a member of enum TokenType")
  }

  case object Default extends TokenType
  case object Service extends TokenType
  case object Batch extends TokenType
  case object DefaultService extends TokenType
  case object DefaultBatch extends TokenType

  val values = findValues
}