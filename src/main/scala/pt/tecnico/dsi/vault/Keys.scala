package pt.tecnico.dsi.vault

import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object Keys {
  implicit val decoder: Decoder[Keys] = deriveDecoder[Keys]
}
case class Keys(keys: List[String])