package pt.tecnico.dsi.vault

import enumeratum._
import io.circe.{Decoder, Encoder}

sealed trait TokenType extends EnumEntry

case object TokenType extends Enum[TokenType] {
  // A little ugly :(
  implicit val encoder: Encoder[TokenType] = (a: TokenType) => Encoder.encodeString(a.entryName.toLowerCase.replace("default", "default-"))
  implicit val decoder: Decoder[TokenType] = Circe.decoderLowercaseOnly(this).prepare(_.withFocus(_.mapString(_.replaceAll("-", ""))))

  case object Service extends TokenType
  case object Batch extends TokenType
  case object DefaultService extends TokenType
  case object DefaultBatch extends TokenType

  val values = findValues
}