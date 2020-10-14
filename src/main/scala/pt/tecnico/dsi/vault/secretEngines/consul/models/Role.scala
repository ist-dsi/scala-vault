package pt.tecnico.dsi.vault.secretEngines.consul.models

import scala.concurrent.duration.Duration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveEncoder, deriveDecoder, renaming}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object Role {
  implicit val encoder: Encoder[Role] = deriveEncoder(renaming.snakeCase)
  implicit val decoder: Decoder[Role] = deriveDecoder(renaming.snakeCase)
}

/**
  * The configurations of a Consul role in Vault.
  * @param policies the list of policies.
  * @param ttl Specifies the TTL for this role. If not provided, the default Vault TTL is used.
  * @param maxTtl Specifies the max TTL for this role. If not provided, the default Vault Max TTL is used.
  * @param tokenType Specifies the type of token to create when using this role.
  * @param local Indicates that the token should not be replicated globally and instead be local to the current datacenter.
  */
case class Role(
  policies: List[String],
  ttl: Duration = Duration.Undefined,
  maxTtl: Duration = Duration.Undefined,
  tokenType: TokenType = TokenType.Client,
  local: Boolean = false,
)