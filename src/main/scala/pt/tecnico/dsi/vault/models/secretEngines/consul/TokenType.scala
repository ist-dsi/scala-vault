package pt.tecnico.dsi.vault.models.secretEngines.consul

import io.circe.{Decoder, Encoder}

object TokenType {
  implicit val encoder: Encoder[TokenType] = Encoder.encodeString.contramap(_.toString.toLowerCase)
  implicit val decoder: Decoder[TokenType] = Decoder.decodeString.emap {
    _.toLowerCase match {
      case "client" => Right(Client)
      case "management" => Right(Management)
      case t => Left(s"Unknown token type $t")
    }
  }
}
sealed trait TokenType
case object Client extends TokenType
case object Management extends TokenType
