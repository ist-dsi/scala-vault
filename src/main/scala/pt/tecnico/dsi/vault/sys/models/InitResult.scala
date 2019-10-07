package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object InitResult {
  implicit val decoder: Decoder[InitResult] = deriveDecoder(renaming.snakeCase, false, None)
}
case class InitResult(keys: Array[String], keysBase64: Array[String], rootToken: String)