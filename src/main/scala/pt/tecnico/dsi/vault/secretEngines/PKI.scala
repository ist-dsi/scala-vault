package pt.tecnico.dsi.vault.secretEngines

import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.secretEngines.pki.models.URLs
import pt.tecnico.dsi.vault.sys.models.{SecretEngine, TuneOptions}

final case class PKI(description: String, config: TuneOptions, options: Map[String, String] = Map.empty, setURLsFromClientURI: Boolean = true,
                     local: Boolean = false, sealWrap: Boolean = false, externalEntropyAccess: Boolean = false) extends SecretEngine {
  val `type` = "pki"
  type Out[F[_]] = pki.PKI[F]
  override def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F] = {
    val pki = vaultClient.secretEngines.pki(path)
    if (setURLsFromClientURI) pki.setURLs(URLs.vaultDefaultsFor(vaultClient.baseUri, path))
    pki
  }
}
