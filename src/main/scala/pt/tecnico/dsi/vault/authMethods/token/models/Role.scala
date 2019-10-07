package pt.tecnico.dsi.vault.authMethods.token.models

import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{DefaultService, TokenType}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

import scala.concurrent.duration.Duration

object Role {
  implicit val encoder: Encoder[Role] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[Role] = deriveDecoder(renaming.snakeCase, false, None)
}
/**
  * @param name The name of the token role.
  * @param orphan If true, tokens created against this policy will be orphan tokens (they will have no parent).
  *               As such, they will not be automatically revoked by the revocation of any other token.
  * @param allowedPolicies If set, tokens can be created with any subset of the policies in this list, rather than the
  *                        normal semantics of tokens being a subset of the calling token's policies. The parameter is
  *                        a comma-delimited string of policy names. If at creation time no_default_policy is not set
  *                        and "default" is not contained in disallowed_policies, the "default" policy will be added to
  *                        the created token automatically.
  * @param disallowedPolicies If set, successful token creation via this role will require that no policies in the given
  *                           list are requested. The parameter is a comma-delimited string of policy names. Adding
  *                           "default" to this list will prevent "default" from being added automatically to created tokens.
  * @param explicitMaxTtl Provides a maximum lifetime for any tokens issued against this role, including periodic tokens.
  *                       Unlike direct token creation, where the value for an explicit max TTL is stored in the token,
  *                       for roles this check will always use the current value set in the role. The main use of this
  *                       is to provide a hard upper bound on periodic tokens, which otherwise can live forever as long
  *                       as they are renewed. This is an integer number of seconds.
  * @param period If specified, the token will be periodic; it will have no maximum TTL (unless an "explicit-max-ttl"
  *               is also set) but every renewal will use the given period. Requires a root/sudo token to use.
  * @param renewable Set to false to disable the ability of the token to be renewed past its initial TTL.
  *                  Setting the value to true will allow the token to be renewable up to the system/mount maximum TTL.
  * @param pathSuffix If set, tokens created against this role will have the given suffix as part of their path in
  *                   addition to the role name. This can be useful in certain scenarios, such as keeping the same role
  *                   name in the future but revoking all tokens created against it before some point in time. The
  *                   suffix can be changed, allowing new callers to have the new suffix as part of their path, and
  *                   then tokens with the old suffix can be revoked via /sys/leases/revoke-prefix.
  * @param tokenType  Specifies the type of tokens that should be returned by the role. If either `service` or `batch` is
  *                   specified, that kind of token will always be returned. If `default-service`, `service` tokens will be
  *                   returned unless the client requests a `batch` type token at token creation time.
  *                   If `default-batch`, `batch` tokens will be returned unless the client requests a `service` type
  *                   token at token creation time.
  */
case class Role(name: String, orphan: Boolean = false,
                allowedPolicies: List[String] = List.empty, disallowedPolicies: List[String] = List.empty,
                explicitMaxTtl: Duration = Duration.Undefined, period: Duration = Duration.Undefined, renewable: Boolean = true,
                /*boundCidrs: List[String],*/ pathSuffix: String = "", tokenType: TokenType = DefaultService)
