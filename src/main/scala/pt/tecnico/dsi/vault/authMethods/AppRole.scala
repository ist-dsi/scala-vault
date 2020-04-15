package pt.tecnico.dsi.vault.authMethods

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.AuthMethod
import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions

final case class AppRole(description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                         local: Boolean = false, sealWrap: Boolean = false) extends AuthMethod {
  val `type` = "approle"
  type Out[T[_]] = approle.AppRole[T]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.authMethods.appRole(path)
}