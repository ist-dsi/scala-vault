package pt.tecnico.dsi.vault.secretEngines.kv.models

import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object Secret {
  implicit def decoder[A: Decoder]: Decoder[Secret[A]] = deriveDecoder(identity)
}

case class Secret[A](data: A, metadata: VersionMetadata)
