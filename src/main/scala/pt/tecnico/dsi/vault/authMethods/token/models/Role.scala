package pt.tecnico.dsi.vault.authMethods.token.models

import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{TokenType, decoderDuration, encodeDuration}

object Role {
  implicit val codec: Codec.AsObject[Role] = deriveCodec(renaming.snakeCase)
}
/**
  * @param allowedPolicies    If set, tokens can be created with any subset of the policies in this list, rather than the
  *                           normal semantics of tokens being a subset of the calling token's policies. The parameter is
  *                           a comma-delimited string of policy names. If at creation time no_default_policy is not set
  *                           and "default" is not contained in disallowed_policies, the "default" policy will be added to
  *                           the created token automatically.
  * @param disallowedPolicies If set, successful token creation via this role will require that no policies in the given
  *                           list are requested. The parameter is a comma-delimited string of policy names. Adding
  *                           "default" to this list will prevent "default" from being added automatically to created tokens.
  * @param orphan If true, tokens created against this policy will be orphan tokens (they will have no parent).
  *               As such, they will not be automatically revoked by the revocation of any other token.
  * @param renewable Set to false to disable the ability of the token to be renewed past its initial TTL.
  *                  Setting the value to true will allow the token to be renewable up to the system/mount maximum TTL.
  * @param pathSuffix If set, tokens created against this role will have the given suffix as part of their path in
  *                   addition to the role name. This can be useful in certain scenarios, such as keeping the same role
  *                   name in the future but revoking all tokens created against it before some point in time. The
  *                   suffix can be changed, allowing new callers to have the new suffix as part of their path, and
  *                   then tokens with the old suffix can be revoked via /sys/leases/revoke-prefix.
  * @param allowedEntityAliases specifies the entity aliases which are allowed to be used during token generation. This field supports globbing.
  * @param tokenBoundCidrs specifies blocks of IP addresses which can authenticate successfully, and ties the resulting token to these blocks as well.
  * @param tokenExplicitMaxTtl Provides a maximum lifetime for any tokens issued against this role, including periodic tokens.
  *                            Unlike direct token creation, where the value for an explicit max TTL is stored in the token,
  *                            for roles this check will always use the current value set in the role. The main use of this
  *                            is to provide a hard upper bound on periodic tokens, which otherwise can live forever as long
  *                            as they are renewed. This is an integer number of seconds.
  * @param tokenNoDefaultPolicy If set, the `default` policy will not be set on generated tokens; otherwise it will be added to
  *                             the policies set in allowed_policies.
  * @param tokenNumUses The maximum number of times a generated token may be used (within its lifetime); 0 means unlimited.
  * @param tokenPeriod  The period, if any, to set on the token.
  * @param tokenType  Specifies the type of tokens that should be returned by the role. If either `service` or `batch` is
  *                   specified, that kind of token will always be returned. If `default-service`, `service` tokens will be
  *                   returned unless the client requests a `batch` type token at token creation time.
  *                   If `default-batch`, `batch` tokens will be returned unless the client requests a `service` type
  *                   token at token creation time.
  */
case class Role(allowedPolicies: List[String] = List.empty, disallowedPolicies: List[String] = List.empty,
                orphan: Boolean = false, renewable: Boolean = true, pathSuffix: String = "",
                allowedEntityAliases: List[String] = List.empty,
                tokenBoundCidrs: List[String] = List.empty, tokenExplicitMaxTtl: Duration = Duration.Undefined,
                tokenNoDefaultPolicy: Boolean = false, tokenNumUses: Int = 0, tokenPeriod: Duration = Duration.Undefined,
                tokenType: TokenType = TokenType.DefaultService)
