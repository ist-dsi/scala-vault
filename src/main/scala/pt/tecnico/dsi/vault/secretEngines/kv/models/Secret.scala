package pt.tecnico.dsi.vault.secretEngines.kv.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Secret {
  implicit def decoder[A: Decoder]: Decoder[Secret[A]] = deriveDecoder(renaming.snakeCase)
}
case class Secret[A](data: A, metadata: VersionMetadata)
