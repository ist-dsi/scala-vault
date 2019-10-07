package pt.tecnico.dsi.vault

import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.{Decoder, Encoder}

object TokenType {
  // A little ugly :(
  implicit val encoder: Encoder[TokenType] = deriveEnumerationEncoder[TokenType].mapJson { json =>
    json.mapString(_.toLowerCase.replace("default", "default-"))
  }
  implicit val decoder: Decoder[TokenType] = deriveEnumerationDecoder[TokenType].prepare(_.withFocus(_.mapString { string =>
    string.split('-').map(_.capitalize).mkString
  }))
}
sealed trait TokenType
case object Service extends TokenType
case object Batch extends TokenType
case object DefaultService extends TokenType
case object DefaultBatch extends TokenType