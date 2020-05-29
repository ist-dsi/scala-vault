package pt.tecnico.dsi.vault.sys.models

import io.circe.Codec
import pt.tecnico.dsi.vault.VaultClient

object SecretEngine {
  implicit val codec: Codec.AsObject[SecretEngine] = Codec.AsObject.from(Mount.decoder(apply), Mount.encoder.contramapObject(identity))

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
      def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = ???
    }
  }
}

trait SecretEngine extends Mount