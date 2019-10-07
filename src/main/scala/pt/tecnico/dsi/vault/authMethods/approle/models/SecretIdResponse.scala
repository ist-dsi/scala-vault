package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object SecretIdResponse {
  implicit val decoder: Decoder[SecretIdResponse] = deriveDecoder(renaming.snakeCase, false, None)
}
case class SecretIdResponse(secretIdAccessor: String, secretId: String)