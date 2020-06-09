package pt.tecnico.dsi.vault.sys.models

import io.circe.Codec
import pt.tecnico.dsi.vault.sys.models.Mount.UnmountableMount

object SecretEngine {
  implicit val codec: Codec.AsObject[SecretEngine] = Mount.codec(apply)

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
  def apply(`type`: String, description: String, config: TuneOptions, options: Map[String, String] = Map.empty,
            local: Boolean = false, sealWrap: Boolean = false, externalEntropyAccess: Boolean = false): SecretEngine =
    new UnmountableMount(`type`, description, config, options, local, sealWrap, externalEntropyAccess) with SecretEngine {}
}
trait SecretEngine extends Mount