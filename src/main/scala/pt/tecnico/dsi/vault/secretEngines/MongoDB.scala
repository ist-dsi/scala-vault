package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.{SecretEngine, TuneOptions}

final case class MongoDB(description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                         local: Boolean = false, sealWrap: Boolean = false) extends SecretEngine {
  val `type` = "database"
  type Out[F[_]] = databases.MongoDB[F]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.secretEngines.mongodb(path)
}
