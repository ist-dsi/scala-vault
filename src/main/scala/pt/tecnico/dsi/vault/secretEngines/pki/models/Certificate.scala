package pt.tecnico.dsi.vault.secretEngines.pki.models

import java.security.cert.X509Certificate

import io.circe.Decoder
import io.circe.derivation._
import pt.tecnico.dsi.vault.secretEngines.pki.PKI._

object Certificate {
  implicit val decoder: Decoder[Certificate] = deriveDecoder(renaming.snakeCase, false, None)
}
//TODO: change this to multiple classes. Select over them using path dependent types.
case class Certificate(serialNumber: String, certificate: X509Certificate, issuingCa: X509Certificate, caChain: Option[Array[X509Certificate]] = None,
                       privateKey: Option[String] = None, privateKeyType: Option[KeySettings.Type] = None) {
  val certificatePem: String = pemEncode(certificate)
  val issuingCaPem: String = pemEncode(issuingCa)
  val caChainPem: Array[String] = caChain.getOrElse(Array.empty).map(pemEncode)

  def pemBundle(): Option[String] = privateKey.map(key => s"$certificatePem\n$key\n$issuingCaPem\n${caChainPem.mkString("\n")}")
}
