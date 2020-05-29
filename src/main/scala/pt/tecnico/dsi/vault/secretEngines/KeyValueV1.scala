package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.{SecretEngine, TuneOptions}

final case class KeyValueV1(description: String, config: TuneOptions, _options: Option[Map[String, String]] = None,
                            local: Boolean = false, sealWrap: Boolean = false) extends SecretEngine {
  val `type` = "kv"
  override val options: Option[Map[String, String]] = _options.map(_ + ("version" -> "1"))
  type Out[T[_]] = kv.KeyValueV1[T]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.secretEngines.keyValueV1(path)
}
