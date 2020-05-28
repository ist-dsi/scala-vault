package pt.tecnico.dsi.vault.sys.models

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object UnsealOptions {
  implicit val encoder: Encoder.AsObject[UnsealOptions] = deriveEncoder(renaming.snakeCase, None)
}
case class UnsealOptions(key: String, reset: Option[Boolean] = None, migrate: Option[Boolean] = None)