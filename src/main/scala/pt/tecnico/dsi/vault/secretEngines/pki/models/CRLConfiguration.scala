package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation._
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import scala.concurrent.duration.Duration

object CRLConfiguration {
  implicit val encoder = deriveEncoder[CRLConfiguration](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[CRLConfiguration](renaming.snakeCase, false, None)
}
case class CRLConfiguration(expiry: Duration = Duration.Undefined, disable: Boolean = false)


