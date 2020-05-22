package pt.tecnico.dsi.vault.authMethods

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.AuthMethod
import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions

final case class Token(description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                       local: Boolean = false, sealWrap: Boolean = false) extends AuthMethod {
  val `type` = "token"
  type Out[T[_]] = token.Token[T]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = vaultClient.authMethods.token
}