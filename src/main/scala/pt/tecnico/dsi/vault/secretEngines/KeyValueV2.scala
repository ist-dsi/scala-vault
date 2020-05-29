package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.{SecretEngine, TuneOptions}

final case class KeyValueV2(description: String, config: TuneOptions, _options: Option[Map[String, String]] = None,
                            local: Boolean = false, sealWrap: Boolean = false) extends SecretEngine {
  val `type` = "kv"
  override val options: Option[Map[String, String]] = Some(_options.getOrElse(Map.empty) + ("version" -> "2"))
  type Out[T[_]] = kv.KeyValueV2[T]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.secretEngines.keyValueV2(path)
}
