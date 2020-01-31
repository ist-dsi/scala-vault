package pt.tecnico.dsi.vault.sys.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions
import pt.tecnico.dsi.vault.{VaultClient, decoderDuration, encodeDuration}

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

  implicit val encoder: Encoder[SecretEngine] =
    Encoder.forProduct6("type", "description", "config", "options", "local", "seal_wrap") { engine =>
      (engine.`type`, engine.config, engine.config, engine.options, engine.local, engine.sealWrap)
    }
  implicit val decoder: Decoder[SecretEngine] = Decoder.forProduct6[SecretEngine, String, String, TuneOptions, Option[Map[String, String]], Boolean, Boolean](
    "type", "description", "config", "options", "local", "seal_wrap")(SecretEngine.apply)

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
            local: Boolean = false, sealWrap: Boolean = false): SecretEngine = {
    // Bulk rename to ensure the apply argument names are the clean ones
    val (_type, _description, _config, _options, _local, _sealWrap) = (`type`, description, config, options, local, sealWrap)
    new SecretEngine {
      override val `type`: String = _type
      override val description: String = _description
      override val config: TuneOptions = _config
      override val options: Option[Map[String, String]] = _options
      override val local: Boolean = _local
      override val sealWrap: Boolean = _sealWrap

      override type Out[_[_]] = Nothing
      def mounted[F[_]](path: String)(implicit vaultClient: VaultClient[F]): Out[F] = ???
    }
  }
}

trait SecretEngine {
  /** Specifies the name of the secret engine type, such as "pki" or "consul". */
  val `type`: String
  /** Specifies a human-friendly description of the secret engine. */
  val description: String
  /** Specifies configuration options for this mount; if set on a specific mount, values will
    * override any global defaults (e.g. the system TTL/Max TTL) */
  val config: TuneOptions
  /** Specifies mount type specific options that are passed to the backend. */
  val options: Option[Map[String, String]]
  /** Specifies if the secrets engine is a local mount only. Local mounts are not replicated
    * nor (if a secondary) removed by replication. */
  val local: Boolean
  /** Enable seal wrapping for the mount, causing values stored by the mount to be wrapped
    * by the seal's encryption capability. */
  val sealWrap: Boolean

  type Out[T[_]]
  def mounted[F[_]](path: String)(implicit vaultClient: VaultClient[F]): Out[F]
}