package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.secretEngines.pki.models.URLs
import pt.tecnico.dsi.vault.sys.models.SecretEngine
import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions

final case class PKI(description: String, config: TuneOptions, options: Option[Map[String, String]] = None,
                     setURLsFromClientURI: Boolean = true, local: Boolean = false, sealWrap: Boolean = false) extends SecretEngine {
  val `type` = "pki"
  type Out[T[_]] = pki.PKI[T]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = {
    val pki = vaultClient.secretEngines.pki(path)
    if (setURLsFromClientURI) pki.setURLs(URLs.vaultDefaultsFor(vaultClient.baseUri, path))
    pki
  }
}
