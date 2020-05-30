package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object BackupKeys {
  implicit val decoder: Decoder[BackupKeys] = deriveDecoder(renaming.snakeCase)
}
case class BackupKeys(nonce: String, keys: Map[String, String])

