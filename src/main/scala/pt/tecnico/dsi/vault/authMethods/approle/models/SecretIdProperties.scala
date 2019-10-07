package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import pt.tecnico.dsi.vault.{encodeArrayAsCSV, decodeArrayAsCSV}

object SecretIdProperties {
  implicit val encoder: Encoder[SecretIdProperties] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[SecretIdProperties] = deriveDecoder(renaming.snakeCase, false, None)
}
case class SecretIdProperties(metadata: String, cidrList: Array[String], tokenBoundCidrs: Array[String])

