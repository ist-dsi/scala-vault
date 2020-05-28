package pt.tecnico.dsi.vault.secretEngines.consul.models

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.duration.Duration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveEncoder, deriveDecoder, renaming}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object Role {
  def base64encode(string: String): String = Base64.getEncoder.encodeToString(string.getBytes(StandardCharsets.UTF_8))
  def base64decode(string: String): String = new String(Base64.getDecoder.decode(string), StandardCharsets.UTF_8)

  implicit val encoder: Encoder[Role] = deriveEncoder[Role](renaming.snakeCase, None).contramap[Role](r => r.copy(policy = base64encode(r.policy)))
  implicit val decoder: Decoder[Role] = deriveDecoder[Role](renaming.snakeCase, true, None).map(r => r.copy(policy = base64decode(r.policy)))
}

/**
  * The configurations of a Consul role in Vault.
  * @param policy Specifies the ACL policy.
  * @param ttl Specifies the TTL for this role. If not provided, the default Vault TTL is used.
  * @param maxTtl Specifies the max TTL for this role. If not provided, the default Vault Max TTL is used.
  * @param tokenType Specifies the type of token to create when using this role.
  */
case class Role(policy: String, ttl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined, tokenType: TokenType = TokenType.Client)