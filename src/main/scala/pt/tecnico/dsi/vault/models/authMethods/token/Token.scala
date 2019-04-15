package pt.tecnico.dsi.vault.models.authMethods.token

import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.models.TokenType

object Token {
  implicit val decoder: Decoder[Token] = deriveDecoder
  implicit val encoder: Encoder[Token] = deriveEncoder
}
case class Token(id: String, accessor: String,
                 creationTime: Int, creationTtl: Int,
                 displayName: String, entityId: String,
                 expireTime: Option[String], explicitMaxTtl: Int, ttl: Int,
                 meta: Option[String],
                 numUses: Int,
                 orphan: Boolean, path: String,
                 policies: List[String], `type`: TokenType)

