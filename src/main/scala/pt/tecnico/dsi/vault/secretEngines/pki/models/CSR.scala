package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object CSR {
  implicit val decoder: Decoder[CSR] = deriveDecoder(renaming.snakeCase, true, None)
}
case class CSR(csr: String, privateKey: Option[String] = None, privateKeyType: Option[KeySettings.Type] = None)