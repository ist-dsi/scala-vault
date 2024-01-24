package pt.tecnico.dsi.vault.secretEngines.pki

import java.math.BigInteger
import java.security.cert.{X509CRL, X509Certificate}
import scala.concurrent.duration.Duration
import scala.util.Try
import cats.Parallel
import cats.effect.Concurrent
import cats.instances.list.*
import cats.instances.try_.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*
import io.circe.{Decoder, JsonObject}
import io.circe.syntax.*
import org.http4s.{EntityDecoder, Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST}
import pt.tecnico.dsi.vault.{Context, DSL, RolesCRUD, encodeDuration}
import pt.tecnico.dsi.vault.secretEngines.pki.PKI.*
import pt.tecnico.dsi.vault.secretEngines.pki.models.*

object PKI {
  import java.io.ByteArrayInputStream
  import java.security.cert.{CertificateException, CertificateFactory, CRLException}
  import java.util.Base64
  import scala.util.Properties
  import scala.util.control.Exception.catching

  def pemEncode(certificate: X509Certificate): String = {
    val lineSeparator = Properties.lineSeparator
    val encoder = Base64.getMimeEncoder(64, lineSeparator.getBytes)
    val encodedCertText = new String(encoder.encode(certificate.getEncoded))
    "-----BEGIN CERTIFICATE-----" + lineSeparator + encodedCertText + lineSeparator + "-----END CERTIFICATE-----"
  }
  def toSerialString(serial: BigInteger): String = serial.toByteArray.map(i => f"$i%02x").mkString("-")

  val x509CertificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
  def parseCertificate(pem: String): Try[X509Certificate] = {
    val strippedCertificate = pem.trim
      .stripPrefix("-----BEGIN CERTIFICATE-----")
      .stripSuffix("-----END CERTIFICATE-----")
    val decodedBytes = Base64.getMimeDecoder.decode(strippedCertificate)
    val inStream = new ByteArrayInputStream(decodedBytes)
    catching(classOf[CertificateException]).withTry {
      x509CertificateFactory.generateCertificate(inStream).asInstanceOf[X509Certificate]
    }
  }
  def parseChain(pem: String): Try[List[X509Certificate]] = {
    pem.trim.split("(?<=-----END CERTIFICATE-----\n)").toList.traverse(parseCertificate)
  }
  def parseCRL(pem: String): Try[X509CRL] = {
    val strippedCertificate = pem.trim
      .stripPrefix("-----BEGIN X509 CRL-----")
      .stripSuffix("-----END X509 CRL-----")
    val decodedBytes = Base64.getMimeDecoder.decode(strippedCertificate)
    val inStream = new ByteArrayInputStream(decodedBytes)
    catching(classOf[CRLException]).withTry {
      x509CertificateFactory.generateCRL(inStream).asInstanceOf[X509CRL]
    }
  }

  implicit val decoderX509Certificate: Decoder[X509Certificate] = Decoder[String].emapTry(parseCertificate)
  val decoderPemCAChain: Decoder[List[X509Certificate]] = Decoder[String].emapTry(parseChain)
}

// TODO: refactor documentation. Extract variables.
/**
  * @define sudoRequired This endpoint requires sudo capabilities.
  */
final class PKI[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header.Raw) { self =>
  private val dsl = new DSL[F] {}
  import dsl.*

  //<editor-fold desc="Configurations">

  // TODO: these read methods return 204 when no CA is configured

  /** Retrieves the CA certificate in a PEM format. */
  val readCACertificatePem: F[String] = execute(GET(uri / "ca" / "pem", token))(EntityDecoder.text[F])
  /** Retrieves the CA certificate. */
  val readCACertificate: F[X509Certificate] = readCACertificatePem.flatMap { pem =>
    implicitly[Concurrent[F]].fromTry(parseCertificate(pem))
  }

  /** Retrieves the CA certificate chain, including the CA in PEM format. */
  val readCACertificateChainPem: F[String] = execute(GET(uri / "ca_chain", token))(EntityDecoder.text[F])
  /** Retrieves the CA certificate chain, including the CA. */
  val readCACertificateChain: F[List[X509Certificate]] = readCACertificateChainPem.flatMap { pem =>
    implicitly[Concurrent[F]].fromTry(parseChain(pem))
  }

  /**
    * This endpoint allows submitting the CA information for the backend via a PEM file containing the CA certificate and its private key, concatenated.
    *
    * May optionally append additional CA certificates. Useful when creating an intermediate CA to ensure a full chain is returned when signing or generating certificates.
    *
    * Not needed if you are generating a self-signed root certificate, and not used if you have a signed intermediate CA certificate with a generated key
    * (use the <code>/pki/intermediate/set-signed</code> endpoint for that).
    *
    * If you have already set a certificate and key, they will be overridden.
    * @param pemBundle Specifies the key and certificate concatenated in PEM format.
    * @return
    */
  def submitCAInformation(pemBundle: String): F[Unit] = execute(POST(Map("pem_bundle" -> pemBundle), uri / "config" / "ca", token))

  /** @return the duration for which the generated CRL should be marked valid. */
  val readCRLConfiguration: F[CRLConfiguration] = execute(GET(uri / "config" / "crl", token))
  /** Sets the duration for which the generated CRL should be marked valid.
    * If the CRL is disabled, it will return a signed but zero-length CRL for any request. If enabled, it will re-build the CRL.
    *
    * Note: Disabling the CRL does not affect whether revoked certificates are stored internally. Certificates that have been
    * revoked when a role's certificate storage is enabled will continue to be marked and stored as revoked until tidy has
    * been run with the desired safety buffer. Re-enabling CRL generation will then result in all such certificates becoming a part of the CRL.
    */
  def setCRLConfiguration(config: CRLConfiguration): F[Unit] = execute(POST(config, uri / "config" / "crl", token))

  /** @return the URLs to be encoded in generated certificates. */
  val readURLs: F[URLs] = executeWithContextData(GET(uri / "config" / "urls", token))
  /**
    * Sets the issuing certificate endpoints, CRL distribution points, and OCSP server endpoints that will
    * be encoded into issued certificates. You can update any of the values at any time without affecting the other existing values.
    * @param urls the urls to use.
    */
  def setURLs(urls: URLs): F[Unit] = execute(POST(urls, uri / "config" / "urls", token))

  /** Retrieves the current CRL in a PEM format. */
  val readCRLPem: F[String] = execute(GET(uri / "crl" / "pem", token))(EntityDecoder.text[F])
  /** Retrieves the current CRL. */
  val readCRL: F[X509CRL] = readCRLPem.flatMap{ pem =>
    implicitly[Concurrent[F]].fromTry(parseCRL(pem))
  }

  /**
    * Forces a rotation of the CRL. This can be used by administrators to cut the size of the CRL if it contains a number
    * of certificates that have now expired, but has not been rotated due to no further certificates being revoked.
    */
  val rotateCRLs: F[Unit] = execute(GET(uri / "crl" / "rotate", token))

  //</editor-fold>

  //<editor-fold desc="Root">

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · certificate
  //   · issuing_ca
  //   · serial_number
  //  Se type = Exported
  //   · private_key
  //   · private_key_type
  /**
    * Generates a new self-signed CA certificate and private key.
    * If `type` is `Exported`, the private key will be returned; if it is `Internal` the private key will not be returned
    * and cannot be retrieved later. Distribution points use the values set via config/urls.
    *
    * As with other issued certificates, Vault will automatically revoke the generated root at the end of its
    * lease period; the CA certificate will sign its own CRL.
    *
    * As of Vault 0.8.1, if a CA cert/key already exists, this function will not overwrite it; it must be deleted first.
    * Previous versions of Vault would overwrite the existing cert/key with new values.
    *
    * @param `type` Specifies the type of the intermediate to create. If `Exported`, the private key will be returned in the response;
    *               if `Internal` the private key will not be returned and cannot be retrieved later.
    * @param names Specifies the names of the certificate (common name and various SANs - Subject Alternative Names)
    * @param subject Specifies subject fields of the certificate.
    * @param ttl Specifies the requested Time To Live (after which the certificate will be expired).
    *            This cannot be larger than the engine's max (or, if set to Undefined, the default, the system max).
    * @param permittedDNSDomains the DNS domains for which certificates are allowed to be issued or signed by this CA certificate.
    *                            Note that subdomains are allowed, as per RFC.
    * @param keySettings Specifies the settings regarding the private key.
    * @param serialNumber Specifies the Serial Number, if any. Otherwise Vault will generate a random serial for you.
    *                     If you want more than one, specify alternative names in `Names` using OID 2.5.4.5.
    * @param maxPathLength Specifies the maximum path length to encode in the generated certificate. -1 means no limit.
    *                      Unless the signing certificate has a maximum path length set, in which case the path length
    *                      is set to one less than that of the signing certificate. A limit of 0 means a literal path length of zero.
    */
  def generateRoot(`type`: Type, names: Names, subject: Subject, ttl: Duration = Duration.Undefined,
                   permittedDNSDomains: Array[String] = Array.empty, keySettings: KeySettings = KeySettings(),
                   serialNumber: Option[String] = None, maxPathLength: Integer = -1): F[Certificate] = {
    val singles = Iterable(
      "ttl" -> ttl.asJson,
      "permitted_dns_domains" -> permittedDNSDomains.asJson,
      "format" -> "pem".asJson,
      "serial_number" -> serialNumber.asJson,
      "max_path_length" -> maxPathLength.asJson,
      "issuer_name" -> "primary".asJson)
    val parts = names.asJsonObject.toIterable ++ subject.asJsonObject.toIterable ++ keySettings.asJsonObject.toIterable ++ singles
    executeWithContextData(POST(JsonObject.fromIterable(parts), uri / "root" / "generate" / `type`.toString.toLowerCase, token))
  }
  /** Deletes the current CA key. The old CA certificate will still be accessible for reading until
    * a new certificate/key are generated or uploaded.
    * $sudoRequired */
  val deleteRoot: F[Unit] = execute(DELETE(uri / "root", token))

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · certificate
  //   · issuing_ca
  //   · serial_number?
  def signSelfIssued(certificatePem: String): F[Certificate] =
    executeWithContextData(POST(Map("certificate" -> certificatePem), uri / "root" / "sign-self-issued", token))
  def signSelfIssued(certificate: X509Certificate): F[Certificate] = signSelfIssued(PKI.pemEncode(certificate))

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · certificate
  //   · issuing_ca
  //   · serial_number
  //   · ca_chain
  /**
    * Signs a new certificate based upon the provided CSR. Values are taken verbatim from the CSR; the only restriction
    * is that this endpoint will refuse to issue an intermediate CA certificate (see the /pki/root/sign-intermediate endpoint for that functionality.)
    * <strong>This is a potentially dangerous endpoint and only highly trusted users should have access.</strong>
    * @param csr Specifies the PEM-encoded CSR.
    * @param role Specifies a role. If set, the following parameters from the role will have effect: ttl, max_ttl, generate_lease, and no_store.
    * @param keyUsage Specifies the allowed key usage constraint on issued certificates.
    *                 Valid values can be found at https://golang.org/pkg/crypto/x509/#KeyUsage - simply drop the KeyUsage part of the value.
    *                 Values are not case-sensitive. To specify no key usage constraints, set this to an empty list.
    * @param extendedKeyUsage Specifies the allowed extended key usage constraint on issued certificates.
    *                         Valid values can be found at https://golang.org/pkg/crypto/x509/#ExtKeyUsage - simply drop the ExtKeyUsage part of the value.
    *                         Values are not case-sensitive. To specify no key usage constraints, set this to an empty list.
    * @param extendedKeyUsageOIDs Specifies the allowed extended key usage OIDs.
    * @param ttl Specifies the requested Time To Live. Cannot be greater than the engine's max_ttl value.
    *            If not provided, the engine's ttl value will be used, which defaults to system values if not explicitly set.
    * @param format Specifies the format for returned data. If `Der` the output is base64 encoded. If `Pem` the output is base64 PEM encoded.
    *               If `Pem_Bundle` the certificate field will contain the private key (if exported) and certificate, concatenated;
    *               if the issuing CA is not a Vault-derived self-signed root, this will be included as well.
    */
  def signVerbatim(csr: String, role: Option[String] = None, keyUsage: Array[String] = Array("DigitalSignature", "KeyAgreement", "KeyEncipherment"),
                   extendedKeyUsage: Array[String] = Array.empty, extendedKeyUsageOIDs: Array[String] = Array.empty,
                   ttl: Duration = Duration.Undefined, format: Format = Format.Pem): F[Certificate] = {
    val body = Map(
      "csr" -> csr.asJson,
      "role" -> role.asJson,
      "key_usage" -> keyUsage.asJson,
      "extended_key_usage" -> extendedKeyUsage.asJson,
      "extended_key_usage_oids" -> extendedKeyUsageOIDs.asJson,
      "ttl" -> ttl.asJson, "format" -> format.asJson
    )
    val path = role.foldLeft(uri / "root" / "sign-verbatim")(_ / _)
    executeWithContextData(POST(body, path, token))
  }

  //</editor-fold>

  //<editor-fold desc="Intermediate">

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · csr
  //  Se type = Exported
  //   · private_key
  //   · private_key_type
  /**
    * Generates a new private key and a CSR for signing. If using Vault as a root, and for many other CAs, the various parameters on the final
    * certificate are set at signing time and may or may not honor the parameters set here.
    *
    * This will overwrite any previously existing CA private key.
    *
    * This is mostly meant as a helper function, and not all possible parameters that can be set in a CSR are supported.
    *
    * @param `type` Specifies the type of the intermediate to create. If `Exported`, the private key will be returned in the response;
    *               if `Internal` the private key will not be returned and cannot be retrieved later.
    * @param names Specifies the names of the certificate (common name and various SANs - Subject Alternative Names)
    * @param subject Specifies subject fields of the certificate.
    * @param keySettings Specifies the settings regarding the private key.
    * @param serialNumber Specifies the Serial Number, if any. Otherwise Vault will generate a random serial for you.
    *                     If you want more than one, specify alternative names in `Names` using OID 2.5.4.5.
    */
  def generateIntermediate(`type`: Type, names: Names, subject: Subject = Subject(),
                           keySettings: KeySettings = KeySettings(), serialNumber: Option[String] = None): F[CSR] = {
    val singles = Iterable(
      "type" -> `type`.asJson,
      "serial_number" -> serialNumber.asJson,
      "format" -> (Format.Pem: Format).asJson
    )
    val parts = names.asJsonObject.toIterable ++ subject.asJsonObject.toIterable ++ keySettings.asJsonObject.toIterable ++ singles
    executeWithContextData(POST(JsonObject.fromIterable(parts), uri / "intermediate" / "generate" / `type`.toString.toLowerCase, token))
  }

  /**
    * Submits the signed CA certificate corresponding to a private key generated via /pki/intermediate/generate.
    * The certificate should be submitted in PEM format; see the documentation for /pki/config/ca for some hints on submitting.
    * @param certificatePEM Specifies the certificate in PEM format. May optionally append additional CA certificates
    *                       to populate the whole chain, which will then enable returning the full chain from issue and sign operations.
    */
  def setSignedIntermediate(certificatePEM: String): F[Unit] =
    execute(POST(Map("certificate" -> certificatePEM), uri / "intermediate" / "set-signed", token))
  def setSignedIntermediate(certificate: X509Certificate): F[Unit] = setSignedIntermediate(PKI.pemEncode(certificate))

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · certificate
  //   · issuing_ca
  //   · serial_number
  //   · ca_chain
  /**
    * Uses the configured CA certificate to issue a certificate with appropriate values for acting as an intermediate CA.
    * Distribution points use the values set via config/urls. Values set in the CSR are ignored unless use_csr_values is
    * set to true, in which case the values from the CSR are used verbatim.
    * @param csr Specifies the PEM-encoded CSR.
    * @param names Specifies the names of the certificate (common name and various SANs - Subject Alternative Names)
    * @param subject Specifies subject fields of the certificate.
    * @param ttl Specifies the requested Time To Live (after which the certificate will be expired). This cannot be larger
    *            than the engine's max (or, if not set, the system max). However, this can be after the expiration of the signing CA.
    * @param permittedDNSDomains the DNS domains for which certificates are allowed to be issued or signed by this CA certificate.
    *                            Note that subdomains are allowed, as per RFC.
    * @param useCsrValues If set to true, then:
    *                     1) Subject information, including names and alternate names, will be preserved from the CSR rather
    *                        than using the values provided in the other parameters to this path;
    *                     2) Any key usages (for instance, non-repudiation) requested in the CSR will be added to the
    *                        basic set of key usages used for CA certs signed by this path;
    *                     3) Extensions requested in the CSR will be copied into the issued certificate.
    * @param serialNumber Specifies the Serial Number, if any. Otherwise Vault will generate a random serial for you.
    *                     If you want more than one, specify alternative names in `Names` using OID 2.5.4.5.
    * @param maxPathLength Specifies the maximum path length to encode in the generated certificate. -1 means no limit.
    *                      Unless the signing certificate has a maximum path length set, in which case the path length
    *                      is set to one less than that of the signing certificate. A limit of 0 means a literal path length of zero.
    */
  def signIntermediate(csr: String, names: Names, subject: Subject = Subject(), ttl: Duration = Duration.Undefined,
                       permittedDNSDomains: Array[String] = Array.empty, useCsrValues: Boolean = false,
                       serialNumber: Option[String] = None, maxPathLength: Integer = -1): F[Certificate] = {
    val singles = Iterable(
      "csr" -> csr.asJson,
      "ttl" -> ttl.asJson,
      "permitted_dns_domains" -> permittedDNSDomains.asJson,
      "use_csr_values" -> useCsrValues.asJson,
      "format" -> (Format.Pem: Format).asJson,
      "serial_number" -> serialNumber.asJson,
      "max_path_length" -> maxPathLength.asJson)
    val parts = names.asJsonObject.toIterable ++ subject.asJsonObject.toIterable ++ singles
    executeWithContextData(POST(JsonObject.fromIterable(parts), uri / "root" / "sign-intermediate", token))
  }

  //</editor-fold>

  //<editor-fold desc="Roles/Certificate">

  object roles extends RolesCRUD[F, Role](path, uri)

  // We could make the result be a dependent type based upon the Type value
  // Devolve com Context:
  //   · certificate
  //   · issuing_ca
  //   · serial_number
  //   · ca_chain
  //  Se type = Exported
  //   · private_key
  //   · private_key_type
  /**
    * Generates a new set of credentials (private key and certificate) based on the role named in the endpoint.
    * The issuing CA certificate is returned as well, so that only the root CA need be in a client's trust store.
    * @param role Specifies the name of the role to create the certificate against.
    * @param names Specifies the names of the certificate (common name and various SANs - Subject Alternative Names)
    * @param ttl Specifies requested Time To Live. Cannot be greater than the role's max_ttl value.
    *            If not provided, the role's ttl value will be used. Note that the role values default to system values if not explicitly set.
    * @param privateKeyFormat Specifies the format for marshaling the private key. Defaults to `Der` which will return key PEM-encoded.
    *                         The other option is `Pkcs8` which will return the key marshalled as PEM-encoded PKCS8.
    */
  def generateCertificate(role: String, names: Names, ttl: Duration = Duration.Undefined,
                          privateKeyFormat: KeySettings.Format = KeySettings.Format.Der): F[Certificate] = {
    val singles = Iterable(
      "ttl" -> ttl.asJson,
      "private_key_format" -> privateKeyFormat.asJson,
      "format" -> (Format.Pem: Format).asJson,
    )
    val body = singles.foldLeft(names.asJsonObject){ case (a, (k, v)) => a.add(k, v)}
    executeWithContextData(POST(body, uri / "issue" / role, token))
  }
  /**
    * Signs a new certificate based upon the provided CSR and the supplied parameters, subject to the restrictions
    * contained in the role named in the endpoint. The issuing CA certificate is returned as well, so that only
    * the root CA need be in a client's trust store.
    * @param role Specifies the name of the role to sign the csr against.
    * @param csr Specifies the PEM-encoded CSR.
    * @param names Specifies the names of the certificate (common name and various SANs - Subject Alternative Names)
    * @param ttl Specifies requested Time To Live. Cannot be greater than the role's max_ttl value.
    *            If not provided, the role's ttl value will be used. Note that the role values default to system values if not explicitly set.
    */
  def signCertificate(role: String, csr: String, names: Names, ttl: Duration = Duration.Undefined): F[Certificate] = {
    val singles = Iterable("csr" -> csr.asJson, "ttl" -> ttl.asJson, "format" -> (Format.Pem: Format).asJson)
    val body = singles.foldLeft(names.asJsonObject){ case (a, (k, v)) => a.add(k, v)}
    executeWithContextData(POST(body, uri / "sign" / role, token))
  }
  
  /** Revokes a certificate using its serial number. This is an alternative option to the standard method of revoking using Vault lease IDs.
    * A successful revocation will rotate the CRL. */
  def revoke(serial: String): F[Unit] = execute(POST(Map("serial_number" -> serial), uri / "revoke", token))
  
  /** Revokes a certificate using its serial number. This is an alternative option to the standard method of revoking using Vault lease IDs.
   * A successful revocation will rotate the CRL. */
  def revoke(certificate: X509Certificate): F[Unit] = revoke(toSerialString(certificate.getSerialNumber))
  
  /**
    * Retrieves the certificate with the given `serial`.
    *
    * To read the CA certificate {@see readCACertificate}.
    * To read the CA chain {@see readCACertificateChain}.
    * To read the CRL {@see readCRL}.
    *
    * @param serial Specifies the serial of the certificate to read. must be hex encoded and hyphen-separated.
    *               See {@see PKI.toSerialString}.
    */
  def readCertificate(serial: String): F[Option[X509Certificate]] = {
    implicit val d = Context.decoder(Decoder[X509Certificate].at("certificate"))
    executeOptionWithContextData[X509Certificate](GET(uri / "cert" / serial))
  }
  
  /** Retrieves the certificate with the given `serial`. */
  def readCertificate(serial: BigInteger): F[Option[X509Certificate]] = readCertificate(toSerialString(serial))
  
  /** Returns the serial numbers of the current certificates. */
  val listCertificatesSerials: F[List[String]] = executeWithContextKeys(LIST(uri / "certs", token))
  
  /** Returns the current certificates. */
  def listCertificates(implicit P: Parallel[F]): F[List[X509Certificate]] = {
    import cats.implicits.*
    listCertificatesSerials.flatMap(_.parTraverseFilter(readCertificate))
  }
  //</editor-fold>
  
  /**
    * Allows tidying up the storage backend and/or CRL by removing certificates that have expired and are past a certain buffer period beyond their expiration time.
    * @param tidyCertStore Specifies whether to tidy up the certificate store.
    * @param tidyRevokedCerts Set to true to expire all revoked and expired certificates, removing them both from the CRL and from storage.
    *                         The CRL will be rotated if this causes any values to be removed.
    * @param safetyBuffer Specifies A duration (given as an integer number of seconds or a string; defaults to 72h) used as
    *                     a safety buffer to ensure certificates are not expunged prematurely; as an example, this can keep
    *                     certificates from being removed from the CRL that, due to clock skew, might still be considered valid on other hosts.
    *                     For a certificate to be expunged, the time must be after the expiration time of the certificate
    *                     (according to the local clock) plus the duration of safety_buffer.
    */
  def tidy(tidyCertStore: Boolean = false, tidyRevokedCerts: Boolean = false, safetyBuffer: Duration = Duration.Undefined): F[Unit] =
    execute(POST(Map(
      "tidy_cert_store" -> tidyCertStore.asJson,
      "tidy_revoked_certs" -> tidyRevokedCerts.asJson,
      "safety_buffer" -> safetyBuffer.asJson,
    ), uri / "tidy", token))
}
