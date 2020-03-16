package pt.tecnico.dsi.vault.secretEngines.databases.models

import io.circe.derivation.{deriveDecoder, renaming}
import io.circe.Decoder

object Credential {
  implicit val decoder: Decoder[Credential] = deriveDecoder[Credential](renaming.snakeCase, false, None)
}
case class Credential(username: String, password: String)
