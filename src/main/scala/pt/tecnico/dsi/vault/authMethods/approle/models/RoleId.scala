package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object RoleId {
  implicit val codec: Codec.AsObject[RoleId] = deriveCodec(renaming.snakeCase, false, None)
}
case class RoleId(roleId: String)
