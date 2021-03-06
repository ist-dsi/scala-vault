package pt.tecnico.dsi.vault.authMethods.approle.models

import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{TokenType, decoderDuration, encodeDuration}

object Role {
  implicit val codec: Codec.AsObject[Role] = deriveCodec(renaming.snakeCase)
}

/**
  * @param bindSecretId Require secret_id to be presented when logging in using this AppRole.
  * @param secretIdBoundCidrs list of CIDR blocks; if set, specifies blocks of IP addresses which can perform the login operation.
  * @param secretIdNumUses Number of times any particular SecretID can be used to fetch a token from this AppRole, after which the SecretID will expire.
  *                        A value of zero will allow unlimited uses.
  * @param secretIdTtl Duration in either an integer number of seconds (3600) or an integer time unit (60m) after which any SecretID expires.
  * @param tokenPolicies list of policies set on tokens issued via this AppRole.
  * @param tokenTtl Duration in either an integer number of seconds (3600) or an integer time unit (60m) to set as the TTL for issued tokens and at renewal time.
  * @param tokenMaxTtl Duration in either an integer number of seconds (3600) or an integer time unit (60m) after which the issued token can no longer be renewed.
  * @param tokenExplicitMaxTtl If set, will encode an explicit max TTL onto the token. This is a hard cap even if token_ttl and
  *                            token_max_ttl would otherwise allow a renewal.
  * @param tokenPeriod Duration in either an integer number of seconds (3600) or an integer time unit (60m). If set, the token
  *               generated using this AppRole is a periodic token; so long as it is renewed it never expires, but the TTL
  *               set on the token at each renewal is fixed to the value specified here. If this value is modified, the
  *               token will pick up the new value at its next renewal.
  * @param tokenBoundCidrs list of CIDR blocks; if set, specifies blocks of IP addresses which can use the auth tokens generated by this role.
  * @param tokenNumUses Number of times issued tokens can be used. A value of 0 means unlimited uses.
  * @param tokenNoDefaultPolicy If set, the `default` policy will not be set on generated tokens; otherwise it will be added to the
  *                             policies set in `tokenPolicies`.
  * @param tokenType The type of token that should be generated via this role. Can be service, batch, or default to use
  *                  the mount's default (which unless changed will be service tokens).
  * @param enableLocalSecretIds If set, the secret IDs generated using this role will be cluster local. This can only be
  *                             set during role creation and once set, it can't be reset later.
  */
case class Role(bindSecretId: Boolean = true,
                secretIdBoundCidrs: List[String] = List.empty, secretIdNumUses: Int = 0, secretIdTtl: Duration = Duration.Undefined,
                tokenPolicies: List[String] = List.empty, tokenTtl: Duration = Duration.Undefined, tokenMaxTtl: Duration = Duration.Undefined,
                tokenExplicitMaxTtl: Duration = Duration.Undefined, tokenPeriod: Duration = Duration.Undefined,
                tokenBoundCidrs: List[String] = List.empty, tokenNumUses: Int = 0, tokenNoDefaultPolicy: Boolean = false,
                tokenType: TokenType = TokenType.Service, enableLocalSecretIds: Boolean = false)