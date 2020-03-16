package pt.tecnico.dsi.vault.sys.models

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

object UnsealOptions {
  implicit val encoder: Encoder.AsObject[UnsealOptions] = deriveEncoder
}
case class UnsealOptions(key: String, reset: Option[Boolean] = None, migrate: Option[Boolean] = None)