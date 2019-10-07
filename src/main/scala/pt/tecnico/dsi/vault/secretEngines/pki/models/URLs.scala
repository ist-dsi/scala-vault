package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation._
import org.http4s.circe._
import org.http4s.Uri

object URLs {
  implicit val encoder = deriveEncoder[URLs](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[URLs](renaming.snakeCase, false, None)

  def apply(issuingCertificateEndpoint: Uri, crlDistributionPointEndpoint: Uri, ocspServerEndpoint: Uri): URLs =
    URLs(Some(Array(issuingCertificateEndpoint)), Some(Array(crlDistributionPointEndpoint)), Some(Array(ocspServerEndpoint)))

  /**
    * Will set URLs to vaultFrontendAddress/pkiPath followed by /ca, and /crl for the issuing certificates endpoint, and
    * CRL distribution endpoint respectively. Does not set the OCSP server endpoint as Vault does not provide a OCSP server.
    * @param vaultFrontendAddress
    * @param pkiPath
    */
  def vaultDefaultsFor(vaultFrontendAddress: Uri, pkiPath: String): URLs = {
    def baseUri(extra: String) = Some(Array(vaultFrontendAddress.withPath(s"${vaultFrontendAddress.path}/$pkiPath/$extra")))
    URLs(baseUri("ca"), baseUri("crl"), None)
  }
}

/**
  * @param issuingCertificates Specifies the URL values for the Issuing Certificate field.
  * @param crlDistributionPoints  Specifies the URL values for the CRL Distribution Points field.
  * @param ocspServers pecifies the URL values for the OCSP Servers field.
  */
case class URLs(issuingCertificates: Option[Array[Uri]] = None, crlDistributionPoints: Option[Array[Uri]] = None, ocspServers: Option[Array[Uri]] = None)


