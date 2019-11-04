package pt.tecnico.dsi.vault.sys.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

object Policy {
  implicit val encoder: Encoder[Policy] = deriveEncoder
  implicit val decoder: Decoder[Policy] = deriveDecoder
}
case class Policy(name: String, rules: String)