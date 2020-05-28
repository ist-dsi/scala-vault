package pt.tecnico.dsi.vault.secretEngines.pki.models

import java.security.cert.X509Certificate
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.vault.secretEngines.pki.PKI.{pemEncode, decoderX509Certificate}

object Certificate {
  implicit val decoder: Decoder[Certificate] = deriveDecoder(renaming.snakeCase, true, None)
}
// Is it worth changing this to multiple classes, and select over them using path dependent types.
// When generating a Certificate with Type = Exported we know for sure the private key exists.
// When signing an intermediate certificate when for sure a caChain exists.

case class Certificate(serialNumber: String, certificate: X509Certificate, issuingCa: X509Certificate, caChain: Option[Array[X509Certificate]] = None,
                       privateKey: Option[String] = None, privateKeyType: Option[KeySettings.Type] = None) {
  val certificatePem: String = pemEncode(certificate)
  val issuingCaPem: String = pemEncode(issuingCa)
  val caChainPem: Array[String] = caChain.getOrElse(Array.empty).map(pemEncode)

  def pemBundle(): Option[String] = privateKey.map(key => s"$certificatePem\n$key\n$issuingCaPem\n${caChainPem.mkString("\n")}")
}
