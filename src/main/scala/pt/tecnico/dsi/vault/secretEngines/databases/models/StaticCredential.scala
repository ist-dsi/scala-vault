package pt.tecnico.dsi.vault.secretEngines.databases.models

import java.time.OffsetDateTime
import io.circe.derivation.{deriveDecoder, renaming}
import scala.concurrent.duration.FiniteDuration
import pt.tecnico.dsi.vault.decoderFiniteDuration

object StaticCredential {
  implicit val decoder = deriveDecoder[StaticCredential](renaming.snakeCase, false, None)
}
case class StaticCredential(username: String, password: String,
                            lastVaultRotation: OffsetDateTime, rotationPeriod: FiniteDuration, ttl: FiniteDuration)
