package pt.tecnico.dsi.vault.authMethods

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.{AuthMethod, TuneOptions}

final case class AppRole(description: String, config: TuneOptions, options: Map[String, String] = Map.empty,
                         local: Boolean = false, sealWrap: Boolean = false) extends AuthMethod {
  val `type` = "approle"
  type Out[F[_]] = approle.AppRole[F]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.authMethods.appRole(path)
}