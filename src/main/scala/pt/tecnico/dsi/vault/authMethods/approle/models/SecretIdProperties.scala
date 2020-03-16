package pt.tecnico.dsi.vault.authMethods.approle.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decodeArrayAsCSV, encodeArrayAsCSV}

object SecretIdProperties {
  implicit val codec: Codec.AsObject[SecretIdProperties] = deriveCodec(renaming.snakeCase, false, None)
}
case class SecretIdProperties(metadata: String, cidrList: Array[String], tokenBoundCidrs: Array[String])

