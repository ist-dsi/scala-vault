package pt.tecnico.dsi.vault.secretEngines.consul.models

import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}

object TokenType {
  // A little ugly :(
  implicit val encoder: Encoder[TokenType] = deriveEnumerationEncoder[TokenType].mapJson(_.mapString(_.toLowerCase))
  implicit val decoder: Decoder[TokenType] = deriveEnumerationDecoder[TokenType].prepare(_.withFocus(_.mapString(_.capitalize)))
}
sealed trait TokenType
case object Client extends TokenType
case object Management extends TokenType
