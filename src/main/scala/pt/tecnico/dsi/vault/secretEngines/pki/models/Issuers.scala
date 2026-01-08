package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Issuers {
  implicit val decoder: Decoder[Issuers] = deriveDecoder(renaming.snakeCase)
}
case class Issuers(importedIssuers: List[String])
