package pt.tecnico.dsi.vault.sys.models

import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.TokenType
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object TuneOptions {
  implicit val codec: Codec.AsObject[TuneOptions] = deriveCodec(renaming.snakeCase)
}
/**
  * @param defaultLeaseTtl The default lease duration, specified as a string duration like "5s" or "30m".
  * @param maxLeaseTtl The maximum lease duration, specified as a string duration like "5s" or "30m".
  * @param forceNoCache Disable caching.
  * @param tokenType the type of tokens that should be returned by the mount. Only usable for Auth Methods.
  * @param listingVisibility Specifies whether to show this mount in the UI-specific listing endpoint.
  * @param auditNonHmacRequestKeys list of keys that will not be HMAC'd by audit devices in the request data object.
  * @param auditNonHmacResponseKeys list of keys that will not be HMAC'd by audit devices in the response data object.
  * @param allowedResponseHeaders list of headers to whitelist, allowing a plugin to include them in the response.
  * @param passthroughRequestHeaders list of headers to whitelist and pass from the request to the plugin.
  */
case class TuneOptions(defaultLeaseTtl: Duration, maxLeaseTtl: Duration = Duration.Zero, forceNoCache: Boolean = false,
                       tokenType: Option[TokenType] = None, listingVisibility: Option[String] = None,
                       auditNonHmacRequestKeys: Option[List[String]] = None, auditNonHmacResponseKeys: Option[List[String]] = None,
                       allowedResponseHeaders: Option[List[String]] = None, passthroughRequestHeaders: Option[List[String]] = None)