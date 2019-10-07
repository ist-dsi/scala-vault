package pt.tecnico.dsi.vault.sys.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{DefaultService, TokenType}
import pt.tecnico.dsi.vault.{encodeDuration, decoderDuration}

import scala.concurrent.duration.Duration

object AuthMethod {
  object TuneOptions {
    implicit val encoder: Encoder[TuneOptions] = deriveEncoder(renaming.snakeCase, None)
    implicit val decoder: Decoder[TuneOptions] = deriveDecoder(renaming.snakeCase, false, None)
  }
  /**
    * @param defaultLeaseTtl The default lease duration, specified as a string duration like "5s" or "30m".
    * @param maxLeaseTtl The maximum lease duration, specified as a string duration like "5s" or "30m".
    * @param auditNonHmacRequestKeys list of keys that will not be HMAC'd by audit devices in the request data object.
    * @param auditNonHmacResponseKeys list of keys that will not be HMAC'd by audit devices in the response data object.
    * @param listingVisibility Specifies whether to show this mount in the UI-specific listing endpoint.
    * @param passthroughRequestHeaders list of headers to whitelist and pass from the request to the plugin.
    * @param allowedResponseHeaders list of headers to whitelist, allowing a plugin to include them in the response.
    * @param tokenType Specifies the type of tokens that should be returned by the mount.
    */
  case class TuneOptions(defaultLeaseTtl: Duration, maxLeaseTtl: Duration = Duration.Undefined,
                         auditNonHmacRequestKeys: Option[List[String]] = None, auditNonHmacResponseKeys: Option[List[String]] = None,
                         listingVisibility: Option[String] = None, passthroughRequestHeaders: Option[List[String]] = None,
                         allowedResponseHeaders: Option[List[String]] = None, tokenType: TokenType = DefaultService)

  implicit val encoder: Encoder[AuthMethod] = deriveEncoder[AuthMethod](renaming.snakeCase, None).mapJsonObject { obj =>
    // The creation and update endpoints for an AuthMethod are different.
    // You can set token_type in an update but not in a create. So we simply remove it from the encoding of AuthMethod
    val jsonObject = obj.filterKeys(_ == "config").mapValues(_.mapObject(_.remove("token_type")))
    obj.add("config", jsonObject.values.head)
  }
  implicit val decoder: Decoder[AuthMethod] = deriveDecoder(renaming.snakeCase, false, None)
}

/**
  * @param `type` Specifies the name of the authentication method type, such as "github" or "token".
  * @param description Specifies a human-friendly description of the auth method.
  */
case class AuthMethod(`type`: String, description: String, config: AuthMethod.TuneOptions,
                      local: Boolean = false, sealWrap: Boolean = false, options: Option[Map[String, String]] = None)