package pt.tecnico.dsi.vault.authMethods.token.models

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{TokenType, decoderDuration, encodeDuration}

object Token {
  implicit val encoder: Encoder[Token] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[Token] = deriveDecoder(renaming.snakeCase, false, None)
}
case class Token(id: String, path: String, accessor: String,
                 creationTime: Long, creationTtl: Duration,
                 displayName: String, entityId: String,
                 expireTime: Option[OffsetDateTime], explicitMaxTtl: Duration, ttl: Duration,
                 numUses: Int, orphan: Boolean,
                 meta: Option[Map[String, String]], policies: List[String], `type`: TokenType)

