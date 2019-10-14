package pt.tecnico.dsi.vault.authMethods.token.models

import scala.concurrent.duration.Duration
import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object CreateOptions {
  implicit val encoder: Encoder[CreateOptions] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[CreateOptions] = deriveDecoder(renaming.snakeCase, false, None)
}
/**
  * @param displayName The display name of the token.
  * @param policies A list of policies for the token. This must be a subset of the policies belonging to the token making the request, unless root.
  *                 If not specified, defaults to all the policies of the calling token.
  * @param noDefaultPolicy If true the default policy will not be contained in this token's policy set.
  * @param noParent If true and set by a root caller, the token will not have the parent token of the caller. This creates a token with no parent.
  * @param numUses The maximum uses for the given token. This can be used to create a one-time-token or limited use token.
  *                The value of 0 has no limit to the number of uses.
  * @param renewable Set to false to disable the ability of the token to be renewed past its initial TTL.
  *                  Setting the value to true will allow the token to be renewable up to the system/mount maximum TTL.
  * @param ttl The TTL period of the token, provided as "1h", where hour is the largest suffix.
  *            If not provided, the token is valid for the default lease TTL, or indefinitely if the root policy is used.
  * @param explicitMaxTtl  If set, the token will have an explicit max TTL set upon it.
  *                        This maximum token TTL cannot be changed later, and unlike with normal tokens, updates to the
  *                        system/mount max TTL value will have no effect at renewal time -- the token will never be
  *                        able to be renewed or used past the value set at issue time.
  * @param period If specified, the token will be periodic; it will have no maximum TTL (unless an "explicit-max-ttl"
  *               is also set) but every renewal will use the given period. Requires a root/sudo token to use.
  * @param id The ID of the client token. Can only be specified by a root token. Otherwise, the token ID is a randomly generated value.
  * @param meta A map of string to string valued metadata. This is passed through to the audit devices.
  */
case class CreateOptions(displayName: String = "token", policies: List[String] = List.empty, noDefaultPolicy: Boolean = false,
                         noParent: Boolean = false, numUses: Int = 0,
                         renewable: Boolean = true, ttl: Duration = Duration.Undefined, explicitMaxTtl: Duration = Duration.Undefined,
                         period: Duration = Duration.Undefined, id: Option[String] = None, meta: Map[String, String] = Map.empty)

