package pt.tecnico.dsi.vault.sys.models

import scala.concurrent.duration.Duration
import io.circe.{Decoder, Encoder}

object SecretEngine {
  object TuneOptions {
    import io.circe.Codec
    import io.circe.derivation.{deriveCodec, renaming}
    import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
    implicit val codec: Codec.AsObject[TuneOptions] = deriveCodec(renaming.snakeCase, true, None)
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

  implicit val encoder: Encoder.AsObject[SecretEngine] = Mount.encoder[TuneOptions].contramapObject[SecretEngine](identity)
  implicit val decoder: Decoder[SecretEngine] = Mount.decoder[TuneOptions]().map(_.asInstanceOf[SecretEngine])

  /**
    * Creates a new Secret Engine using the provided settings. This secret engine will throw a NotImplementedError
    * inside the `mounted` method.
    * @param `type` Specifies the name of the secret engine type, such as "pki" or "consul".
    * @param description Specifies a human-friendly description of the secret engine.
    * @param config Specifies configuration options for this mount; if set on a specific mount, values will
    *               override any global defaults (e.g. the system TTL/Max TTL)
    * @param options Specifies mount type specific options that are passed to the backend.
    * @param local Specifies if the secrets engine is a local mount only. Local mounts are not replicated
    *              nor (if a secondary) removed by replication.
    * @param sealWrap Enable seal wrapping for the mount, causing values stored by the mount to be wrapped
    *                 by the seal's encryption capability.
    */
  def apply(`type`: String, description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
            local: Boolean = false, sealWrap: Boolean = false): SecretEngine =
    Mount(`type`, description, config, options, local, sealWrap).asInstanceOf[SecretEngine]
}

trait SecretEngine extends Mount[SecretEngine.TuneOptions]