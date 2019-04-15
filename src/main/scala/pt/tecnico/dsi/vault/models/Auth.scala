package pt.tecnico.dsi.vault.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._

object Auth {
  implicit val decoder: Decoder[Auth] = deriveDecoder
  implicit val encoder: Encoder[Auth] = deriveEncoder
}

case class Auth(clientToken: String, accessor: String,
                policies: List[String], tokenPolicies: List[String],
                leaseDuration: Int, renewable: Boolean,
                entityId: String, tokenType: TokenType,
                metadata: Map[String, String])