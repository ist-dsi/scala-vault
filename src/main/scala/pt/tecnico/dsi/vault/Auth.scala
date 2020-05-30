package pt.tecnico.dsi.vault

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Auth {
  implicit val decoder: Decoder[Auth] = deriveDecoder(renaming.snakeCase)
}
case class Auth(clientToken: String, accessor: String,
                policies: List[String], tokenPolicies: List[String],
                leaseDuration: Int, renewable: Boolean,
                entityId: String, tokenType: TokenType,
                metadata: Map[String, String])