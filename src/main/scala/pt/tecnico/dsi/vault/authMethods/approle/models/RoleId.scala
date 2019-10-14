package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object RoleId {
  implicit val encoder: Encoder[RoleId] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[RoleId] = deriveDecoder(renaming.snakeCase, false, None)
}
case class RoleId(roleId: String)
