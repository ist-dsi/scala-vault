package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.{SecretEngine, TuneOptions}

final case class KeyValueV1(description: String, config: TuneOptions, _options: Map[String, String] = Map.empty,
                            local: Boolean = false, sealWrap: Boolean = false, externalEntropyAccess: Boolean = false) extends SecretEngine {
  val `type` = "kv"
  override val options: Map[String, String] = _options + ("version" -> "1")
  type Out[F[_]] = kv.KeyValueV1[F]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.secretEngines.keyValueV1(path)
}
