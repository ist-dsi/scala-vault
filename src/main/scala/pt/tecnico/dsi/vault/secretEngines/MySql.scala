package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.SecretEngine
import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions

final case class MySql(description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                       local: Boolean = false, sealWrap: Boolean = false) extends SecretEngine {
  val `type` = "database"
  type Out[T[_]] = databases.MySql[T]
  override def mounted[F[_]](path: String)(implicit vaultClient: VaultClient[F]): Out[F] =
    vaultClient.secretEngines.mysql(path)
}
