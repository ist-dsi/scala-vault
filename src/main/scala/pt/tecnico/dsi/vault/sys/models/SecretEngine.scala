package pt.tecnico.dsi.vault.sys.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

import scala.concurrent.duration.Duration

object SecretEngine {
  object TuneOptions {
    implicit val encoder: Encoder[TuneOptions] = deriveEncoder(renaming.snakeCase, None)
    implicit val decoder: Decoder[TuneOptions] = deriveDecoder(renaming.snakeCase, false, None)
  }

  /**
    * @param defaultLeaseTtl The default lease duration, specified as a string duration like "5s" or "30m".
    * @param maxLeaseTtl The maximum lease duration, specified as a string duration like "5s" or "30m".
    * @param forceNoCache Disable caching.
    * @param auditNonHmacRequestKeys list of keys that will not be HMAC'd by audit devices in the request data object.
    * @param auditNonHmacResponseKeys list of keys that will not be HMAC'd by audit devices in the response data object.
    * @param listingVisibility Specifies whether to show this mount in the UI-specific listing endpoint.
    * @param passthroughRequestHeaders list of headers to whitelist and pass from the request to the plugin.
    * @param allowedResponseHeaders list of headers to whitelist, allowing a plugin to include them in the response.
    * @param options Specifies mount type specific options that are passed to the backend.
    */
  case class TuneOptions(defaultLeaseTtl: Duration, maxLeaseTtl: Duration = Duration.Zero, forceNoCache: Boolean = false,
                         auditNonHmacRequestKeys: Option[List[String]] = None, auditNonHmacResponseKeys: Option[List[String]] = None,
                         listingVisibility: Option[String] = None, passthroughRequestHeaders: Option[List[String]] = None,
                         allowedResponseHeaders: Option[List[String]] = None, options: Option[Map[String, String]] = None)

  implicit val encoder: Encoder[SecretEngine] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[SecretEngine] = deriveDecoder(renaming.snakeCase, false, None)
}

/**
  * @param `type` Specifies the name of the authentication method type, such as "github" or "token".
  * @param description Specifies a human-friendly description of the auth method.
  */
case class SecretEngine(`type`: String, description: String, config: SecretEngine.TuneOptions,
                      local: Boolean = false, sealWrap: Boolean = false, options: Option[Map[String, String]] = None)