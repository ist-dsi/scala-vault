package pt.tecnico.dsi.vault.secretEngines.identity.models

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Alias {
  implicit val encoder: Encoder[Alias] = deriveEncoder(renaming.snakeCase)
}
case class Alias(name: String, canonicalId: String, mountAccessor: String, id: Option[String] = None)
