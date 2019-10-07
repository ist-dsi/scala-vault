package pt.tecnico.dsi.vault.authMethods.token.models

import java.time.OffsetDateTime

import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.TokenType
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import scala.concurrent.duration.Duration

object Token {
  implicit val encoder: Encoder[Token] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[Token] = deriveDecoder(renaming.snakeCase, false, None)
}
case class Token(id: String, accessor: String,
                 creationTime: OffsetDateTime, creationTtl: Duration,
                 displayName: String, entityId: String,
                 expireTime: Option[String], explicitMaxTtl: Duration, ttl: Duration,
                 meta: Option[String],
                 numUses: Int,
                 orphan: Boolean, path: String,
                 policies: List[String], `type`: TokenType)

