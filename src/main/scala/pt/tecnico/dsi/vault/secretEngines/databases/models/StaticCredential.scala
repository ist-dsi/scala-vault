package pt.tecnico.dsi.vault.secretEngines.databases.models

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.vault.decoderFiniteDuration

object StaticCredential {
  implicit val decoder: Decoder[StaticCredential] = deriveDecoder[StaticCredential](renaming.snakeCase, false, None)
}
case class StaticCredential(username: String, password: String,
                            lastVaultRotation: OffsetDateTime, rotationPeriod: FiniteDuration, ttl: FiniteDuration)
