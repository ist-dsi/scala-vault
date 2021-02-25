package pt.tecnico.dsi.vault.secretEngines.kv.models

import scala.annotation.nowarn
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Secret {
  // nowarn because of a false negative from the compiler. The Decoder is being inside the macro deriveDecoder.
  implicit def decoder[A](implicit @nowarn d: Decoder[A]): Decoder[Secret[A]] = deriveDecoder(renaming.snakeCase)
}
case class Secret[A](data: A, metadata: VersionMetadata)
