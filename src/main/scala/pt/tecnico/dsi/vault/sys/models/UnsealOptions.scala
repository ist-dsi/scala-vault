package pt.tecnico.dsi.vault.sys.models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object UnsealOptions {
  implicit val encoder: Encoder[UnsealOptions] = deriveEncoder
}
case class UnsealOptions(key: String, reset: Option[Boolean] = None, migrate: Option[Boolean] = None)