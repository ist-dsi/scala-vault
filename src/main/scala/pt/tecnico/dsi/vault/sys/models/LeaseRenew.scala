package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import scala.concurrent.duration.Duration
import pt.tecnico.dsi.vault.decoderDuration

object LeaseRenew {
  implicit val decoder: Decoder[LeaseRenew] = deriveDecoder(renaming.snakeCase, false, None)
}
case class LeaseRenew(leaseId: String, renewable: Boolean, leaseDuration: Duration)