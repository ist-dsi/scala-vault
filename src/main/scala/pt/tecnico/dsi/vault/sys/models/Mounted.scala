package pt.tecnico.dsi.vault.sys.models

import java.util.UUID
import pt.tecnico.dsi.vault.VaultClient
import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object Mounted {
  implicit val decoder: Decoder[Mounted] = deriveDecoder(identity)
}
/**
  * Represents a mounted mount.
  * @param accessor the mount point accessor.
  * @param uuid the uuid of the mount.
  * @param `type` the type of this mount, such as "approle" (authentication method) or "pki" (secret engine).
  * @param description human-friendly description of this mount.
  * @param config configuration options for this mount.
  * @param options mount type specific options.
  * @param local whether this is a local mount only. Local mounts are not replicated nor (if a secondary) removed by replication.
  * @param sealWrap whether this mount seal wraps causing values.
  * @param externalEntropyAccess whether the mount has access to Vault's external entropy source.
  */
case class Mounted(
  accessor: String,
  uuid: UUID,
  `type`: String,
  description: String,
  config: TuneOptions,
  options: Map[String, String] = Map.empty,
  local: Boolean = false,
  sealWrap: Boolean = false,
  externalEntropyAccess: Boolean = false,
) extends Mount {
  type Out[_[_]] = Nothing
  def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = ???
}