package pt.tecnico.dsi.vault.sys.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Policy {
  implicit val codec: Codec.AsObject[Policy] = deriveCodec(renaming.snakeCase, false, None)
}
case class Policy(name: String, rules: String)