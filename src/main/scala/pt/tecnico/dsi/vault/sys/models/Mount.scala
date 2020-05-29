package pt.tecnico.dsi.vault.sys.models

import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.VaultClient

object Mount {
  private[models] def encoder[TuneOptions: Encoder]: Encoder.AsObject[Mount[TuneOptions]] =
    Encoder.forProduct6("type", "description", "config", "options", "local", "seal_wrap") { engine =>
      (engine.`type`, engine.description, engine.config, engine.options, engine.local, engine.sealWrap)
    }
  private[models] def decoder[TuneOptions: Decoder](): Decoder[Mount[TuneOptions]] = Decoder.forProduct6(
    "type", "description", "config", "options", "local", "seal_wrap")(apply[TuneOptions])

  private[models] def apply[TuneOptions](`type`: String, description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                                         local: Boolean = false, sealWrap: Boolean = false): Mount[TuneOptions] = {
    // Bulk rename to ensure the apply argument names are the clean ones
    val (_type, _description, _config, _options, _local, _sealWrap) = (`type`, description, config, options, local, sealWrap)
    new Mount[TuneOptions] {
      override val `type`: String = _type
      override val description: String = _description
      override val config: TuneOptions = _config
      override val options: Option[Map[String, String]] = _options
      override val local: Boolean = _local
      override val sealWrap: Boolean = _sealWrap

      override type Out[_[_]] = Nothing
      def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = ???
    }
  }
}

trait Mount[TuneOptions] {
  /** Specifies the type of this mount, such as "approle" (authentication method) or "pki" (secret engine). */
  val `type`: String
  /** Specifies a human-friendly description of this mount. */
  val description: String
  /** Specifies configuration options for this mount; if set on a specific mount, values will
    * override any global defaults (e.g. the system TTL/Max TTL) */
  val config: TuneOptions
  /** Specifies mount type specific options that are passed to the backend. */
  val options: Option[Map[String, String]]
  /** Specifies if this is a local mount only. Local mounts are not replicated nor (if a secondary) removed by replication. */
  val local: Boolean
  /** Enable seal wrapping for the mount, causing values stored by the mount to be wrapped by the seal's encryption capability. */
  val sealWrap: Boolean

  type Out[T[_]]
  def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F]
}