package pt.tecnico.dsi.vault.secretEngines.pki.models

import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object CRLConfiguration {
  implicit val codec: Codec.AsObject[CRLConfiguration] = deriveCodec(renaming.snakeCase, true, None)
}
case class CRLConfiguration(expiry: Duration = Duration.Undefined, disable: Boolean = false)


