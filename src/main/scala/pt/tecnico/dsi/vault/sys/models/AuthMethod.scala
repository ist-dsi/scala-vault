package pt.tecnico.dsi.vault.sys.models

import scala.concurrent.duration.Duration
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions
import pt.tecnico.dsi.vault.{DefaultService, TokenType, VaultClient, decoderDuration, encodeDuration}

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

  implicit val encoder: Encoder[AuthMethod] = {
    Encoder.forProduct6("type", "description", "config", "options", "local", "seal_wrap") { auth: AuthMethod =>
      (auth.`type`, auth.config, auth.config, auth.options, auth.local, auth.sealWrap)
    }.mapJsonObject { obj =>
      // When tunning (updating) an AuthMethod you can set token_type. However when you are enabling an AuthMethod you cannot.
      // So we simply remove token_type from the encoding of AuthMethod tune configuration.
      val jsonObject = obj.filterKeys(_ == "config").mapValues(_.mapObject(_.remove("token_type")))
      obj.add("config", jsonObject.values.head)
    }
  }

  implicit val decoder: Decoder[AuthMethod] = Decoder.forProduct6[AuthMethod, String, String, TuneOptions, Option[Map[String, String]], Boolean, Boolean](
    "type", "description", "config", "options", "local", "seal_wrap")(AuthMethod.apply)

  /**
    * Creates a new Authentication Method using the provided settings. This authentication method will throw a
    * NotImplementedError inside the `mounted` method.
    *
    * @param `type` Specifies the name of the authentication method type, such as "github" or "token".
    * @param description Specifies a human-friendly description of the auth method.
    * @param config Specifies configuration options for this mount; if set on a specific mount, values will
    *               override any global defaults (e.g. the system TTL/Max TTL)
    * @param options Specifies mount type specific options that are passed to the backend.
    * @param local Specifies if the secrets engine is a local mount only. Local mounts are not replicated
    *              nor (if a secondary) removed by replication.
    * @param sealWrap Enable seal wrapping for the mount, causing values stored by the mount to be wrapped
    *                 by the seal's encryption capability.
    */
  def apply(`type`: String, description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
            local: Boolean = false, sealWrap: Boolean = false): AuthMethod = {
    // Bulk rename to ensure the apply argument names are the clean ones
    val (_type, _description, _config, _options, _local, _sealWrap) = (`type`, description, config, options, local, sealWrap)
    new AuthMethod {
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

trait AuthMethod {
  /** Specifies the name of the authentication method type, such as "github" or "token". */
  val `type`: String
  /** Specifies a human-friendly description of the auth method. */
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