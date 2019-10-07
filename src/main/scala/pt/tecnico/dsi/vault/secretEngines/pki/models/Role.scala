package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation._
import pt.tecnico.dsi.vault.secretEngines.pki.models.KeySettings.RSA
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

import scala.concurrent.duration.Duration

object Role {
  implicit val encoder = deriveEncoder[Role](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[Role](renaming.snakeCase, false, None)
}

/**
  * @param ttl Duration in either an integer number of seconds (3600) or an integer time unit (60m) to set as the TTL for issued tokens and at renewal time.
  * @param maxTtl Duration in either an integer number of seconds (3600) or an integer time unit (60m) after which the issued token can no longer be renewed.
  * @param allowLocalhost Specifies if clients can request certificates for localhost as one of the requested common names. This is useful for testing and
  *                       to allow clients on a single host to talk securely.
  * @param allowBareDomains Specifies if clients can request certificates matching the value of the actual domains themselves; e.g. if a configured domain set
  *                         with allowed_domains is example.com, this allows clients to actually request a certificate containing the name example.com as one
  *                         of the DNS values on the final certificate. In some scenarios, this can be considered a security risk.
  * @param allowSubdomains Specifies if clients can request certificates with CNs that are subdomains of the CNs allowed by the other role options.
  *                        This includes wildcard subdomains. For example, an allowed_domains value of example.com with this option set to true
  *                        will allow foo.example.com and bar.example.com as well as *.example.com. This is redundant when using the allow_any_name option.
  * @param allowGlobDomains Allows names specified in allowed_domains to contain glob patterns (e.g. ftp*.example.com).
  *                         Clients will be allowed to request certificates with names matching the glob patterns.
  * @param allowAnyName Specifies if clients can request any CN. Useful in some circumstances, but make sure you understand
  *                     whether it is appropriate for your installation before enabling it.
  * @param allowIPSans Specifies if clients can request IP Subject Alternative Names. No authorization checking is performed
  *                    except to verify that the given values are valid IP addresses.
  * @param allowedDomains Specifies the domains of the role. This is used with the allow_bare_domains and allow_subdomains options.
  * @param allowedUriSans Defines allowed URI Subject Alternative Names. No authorization checking is performed except to
  *                       verify that the given values are valid URIs. This can be a comma-delimited list or a JSON string slice.
  *                       Values can contain glob patterns (e.g. spiffe://hostname/).
  * @param allowedOtherSans  Defines allowed custom OID/UTF8-string SANs. This field supports globbing. The format is the
  *                          same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8 (or UTF-8).
  *                          This can be a comma-delimited list or a JSON string slice. All values, including globbing values,
  *                          must use the correct syntax, with the exception being a single * which allows any OID and any value (but type must still be UTF8).
  * @param serverFlag Specifies if certificates are flagged for server use.
  * @param clientFlag Specifies if certificates are flagged for client use.
  * @param codeSigningFlag Specifies if certificates are flagged for code signing use.
  * @param emailProtectionFlag Specifies if certificates are flagged for email protection use.
  * @param keyType Specifies the type of key to generate for generated private keys and the type of key expected for submitted CSRs.
  *                Currently, rsa and ec are supported, or when signing CSRs any can be specified to allow keys of either type and
  *                with any bit size (subject to > 1024 bits for RSA keys).
  * @param keyBits Specifies the number of bits to use for the generated keys. This will need to be changed for ec keys.
  *                See https://golang.org/pkg/crypto/elliptic/#Curve for an overview of allowed bit lengths for ec.
  * @param keyUsage Specifies the allowed key usage constraint on issued certificates. Valid values can be found at https://golang.org/pkg/crypto/x509/#KeyUsage
  *                 simply drop the KeyUsage part of the value. Values are not case-sensitive. To specify no key usage constraints, set this to an empty list.
  * @param extKeyUsage Specifies the allowed extended key usage constraint on issued certificates. Valid values can be found at https://golang.org/pkg/crypto/x509/#ExtKeyUsage
  *                    simply drop the ExtKeyUsage part of the value. Values are not case-sensitive. To specify no key usage constraints, set this to an empty list.
  * @param extKeyUsageOids (string: "") - A comma-separated string or list of extended key usage oids.
  * @param useCsrCommonName When used with the CSR signing endpoint, the common name in the CSR will be used instead of taken from the JSON data.
  *                         This does not include any requested SANs in the CSR; use useCsrSans for that.
  * @param useCsrSans When used with the CSR signing endpoint, the subject alternate names in the CSR will be used instead of taken from the JSON data.
  *                   This does not include the common name in the CSR; use useCsrCommonName for that.
  * @param subject Specifies subject fields of the certificate.
  * @param serialNumber Specifies the Serial Number, if any. Otherwise Vault will generate a random serial for you.
  *                     If you want more than one, specify alternative names in the altNames map using OID 2.5.4.5.
  * @param generateLease Specifies if certificates issued/signed against this role will have Vault leases attached to them.
  *                      Certificates can be added to the CRL by vault revoke <leaseId> when certificates are associated with leases.
  *                      It can also be done using the pki/revoke endpoint. However, when lease generation is disabled,
  *                      invoking pki/revoke would be the only way to add the certificates to the CRL.
  * @param noStore If set, certificates issued/signed against this role will not be stored in the storage backend.
  *                This can improve performance when issuing large numbers of certificates. However, certificates issued
  *                in this way cannot be enumerated or revoked, so this option is recommended only for certificates that are
  *                non-sensitive, or extremely short-lived. This option implies a value of false for generateLease.
  * @param requireCn If set to false, makes the commonName field optional while generating a certificate.
  * @param policyIdentifiers A comma-separated string or list of policy OIDs.
  * @param basicConstraintsValidForNonCa Mark Basic Constraints valid when issuing non-CA certificates.
  * @param notBeforeDuration Specifies the duration by which to backdate the NotBefore property.
  */
case class Role(ttl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined,
                allowLocalhost: Boolean = true, allowBareDomains: Boolean = true, allowSubdomains: Boolean = true,
                allowGlobDomains: Boolean = true, allowAnyName: Boolean = false, allowIPSans: Boolean = true,
                allowedDomains: Seq[String] = Seq.empty, allowedUriSans: Seq[String] = Seq.empty, allowedOtherSans: Seq[String] = Seq.empty,
                serverFlag: Boolean = true, clientFlag: Boolean = true, codeSigningFlag: Boolean = false, emailProtectionFlag: Boolean = false,
                keyType: KeySettings.Type = RSA, keyBits: Int = 2048, keyUsage: Seq[String] = Seq("DigitalSignature", "KeyAgreement", "KeyEncipherment"),
                extKeyUsage: Seq[String] = Seq.empty, extKeyUsageOids: String = "",
                useCsrCommonName: Boolean = true, useCsrSans: Boolean = true, requireCn: Boolean = true, subject: Subject, serialNumber: String = "",
                generateLease: Boolean = false, noStore: Boolean = false,
                policyIdentifiers: Seq[String] = Seq.empty, basicConstraintsValidForNonCa: Boolean = false,
                notBeforeDuration: Duration = Duration.Undefined)