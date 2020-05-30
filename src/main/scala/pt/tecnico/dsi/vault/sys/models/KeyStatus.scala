package pt.tecnico.dsi.vault.sys.models

import java.time.OffsetDateTime
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object KeyStatus {
  implicit val decoder: Decoder[KeyStatus] = deriveDecoder(renaming.snakeCase)
}

/**
  * Information about the current encryption key of Vault.
  * @param term the sequential key number
  * @param installTime the time that encryption key was installed
  */
case class KeyStatus(term: Int, installTime: OffsetDateTime)
