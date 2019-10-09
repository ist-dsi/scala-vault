package pt.tecnico.dsi.vault.secretEngines.databases.models

import io.circe.derivation._

object Credential {
  implicit val decoder = deriveDecoder[Credential](renaming.snakeCase, false, None)
}
case class Credential(username: String, password: String)
