package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object RoleId {
  implicit val encoder: Encoder[RoleId] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[RoleId] = deriveDecoder(renaming.snakeCase, false, None)
}
case class RoleId(roleId: String)
