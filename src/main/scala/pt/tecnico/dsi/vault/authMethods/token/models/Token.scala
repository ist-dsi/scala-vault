package pt.tecnico.dsi.vault.authMethods.token.models

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration, TokenType}

object Token {
  implicit val codec: Codec.AsObject[Token] = deriveCodec(renaming.snakeCase)
}
case class Token(id: String, path: String, accessor: String,
                 creationTime: Long, creationTtl: Duration,
                 displayName: String, entityId: String,
                 expireTime: Option[OffsetDateTime], explicitMaxTtl: Duration, ttl: Duration,
                 numUses: Int, orphan: Boolean,
                 meta: Option[Map[String, String]], policies: List[String], `type`: TokenType) {
  def hasExpired(currentDateTime: OffsetDateTime = OffsetDateTime.now()): Boolean =
    expireTime.exists(t => currentDateTime.isAfter(t))
}