package pt.tecnico.dsi.vault.sys.models

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.vault.decoderDuration

object Lease {
  implicit val decoder: Decoder[Lease] = deriveDecoder(renaming.snakeCase)
}
case class Lease(id: String, issueTime: OffsetDateTime, expireTime: Option[OffsetDateTime],
                 lastRenewal: Option[OffsetDateTime], renewable: Boolean, ttl: Duration = Duration.Undefined)