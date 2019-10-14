package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.Decoder
import io.circe.derivation._

object CSR {
  implicit val decoder: Decoder[CSR] = deriveDecoder(renaming.snakeCase, false, None)
}
case class CSR(csr: String, privateKey: Option[String] = None, privateKeyType: Option[KeySettings.Type] = None)