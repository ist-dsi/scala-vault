package pt.tecnico.dsi.vault.sys.models

import io.circe.{Codec, Encoder}
import pt.tecnico.dsi.vault.VaultClient

object Mount {
  def codec[T <: Mount](f: (String, String, TuneOptions, Map[String, String], Boolean, Boolean, Boolean) => T): Codec.AsObject[T] = {
    // When tuning (updating) a Mount you can set token_type in TuneOptions, however when you are enabling it you cannot.
    // So we simply remove token_type from the encoded json.
    implicit val e: Encoder.AsObject[TuneOptions] = (a: TuneOptions) => TuneOptions.codec.encodeObject(a).remove("token_type")
    Codec.forProduct7("type", "description", "config", "options", "local", "seal_wrap", "external_entropy_access")(f)(mount =>
      (mount.`type`, mount.description, mount.config, mount.options, mount.local, mount.sealWrap, mount.externalEntropyAccess)
    )
  }

  private[models] abstract class UnmountableMount(_type: String, _description: String, _config: TuneOptions, _options: Map[String, String] = Map.empty,
                                                  _local: Boolean, _sealWrap: Boolean, _externalEntropyAccess: Boolean) extends Mount {
    override val `type`: String = _type
    override val description: String = _description
    override val config: TuneOptions = _config
    override val options: Map[String, String] = _options
    override val local: Boolean = _local
    override val sealWrap: Boolean = _sealWrap
    override val externalEntropyAccess: Boolean = _externalEntropyAccess

    type Out[_[_]] = Nothing
    def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = ???
  }
}

trait Mount {
  /** Specifies the type of this mount, such as "approle" (authentication method) or "pki" (secret engine). */
  val `type`: String
  /** Specifies a human-friendly description of this mount. */
  val description: String
  /** Specifies configuration options for this mount; if set on a specific mount, values will
    * override any global defaults (e.g. the system TTL/Max TTL) */
  val config: TuneOptions
  /** Specifies mount type specific options that are passed to the backend. */
  val options: Map[String, String]
  /** Specifies if this is a local mount only. Local mounts are not replicated nor (if a secondary) removed by replication. */
  val local: Boolean
  /** Enable seal wrapping for the mount, causing values stored by the mount to be wrapped by the seal's encryption capability. */
  val sealWrap: Boolean
  /** Enable the mount to access Vault's external entropy source. */
  val externalEntropyAccess: Boolean

  type Out[F[_]]
  def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F]
}