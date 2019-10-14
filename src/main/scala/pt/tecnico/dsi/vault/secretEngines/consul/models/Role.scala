package pt.tecnico.dsi.vault.secretEngines.consul.models

import scala.concurrent.duration.Duration
import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object Role {
  implicit val encoder: Encoder[Role] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[Role] = deriveDecoder(renaming.snakeCase, false, None)
}

/**
  * The configurations of a Consul role in Vault.
  * @param policy Specifies the ACL policy.
  * @param ttl Specifies the TTL for this role. If not provided, the default Vault TTL is used.
  * @param maxTtl Specifies the max TTL for this role. If not provided, the default Vault Max TTL is used.
  * @param tokenType Specifies the type of token to create when using this role.
  */
case class Role(policy: Policy, ttl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined, tokenType: TokenType = Client)