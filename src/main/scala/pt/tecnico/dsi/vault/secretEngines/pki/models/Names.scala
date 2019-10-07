package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation._
import pt.tecnico.dsi.vault.{encodeArrayAsCSV, decodeArrayAsCSV}

object Names {
  implicit val encoder = deriveEncoder[Names](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[Names](renaming.snakeCase, false, None)
}

/**
  * @param commonName the requested CN for the certificate. If the CN is allowed by role policy, it will be issued.
  * @param altNames the requested Subject Alternative Names. These can be host names or email addresses; they will be
  *                 parsed into their respective fields. If any requested names do not match role policy, the entire request will be denied.
  * @param ipSans the requested IP Subject Alternative Names. Only valid if the role allows IP SANs (which is the default).
  * @param uriSans the requested URI Subject Alternative Names. If any requested URIs do not match role policy, the entire request will be denied.
  * @param otherSans custom OID/UTF8-string SANs. These must match values specified on the role in allowed_other_sans (globbing allowed).
  *                  The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8.
  * @param excludeCnFromSans If set, the given commonName will not be included in DNS or Email Subject Alternate Names (as appropriate).
  *                          Useful if the CN is not a hostname or email address, but is instead some human-readable identifier.
  */
case class Names(commonName: String, altNames: Array[String] = Array.empty, ipSans: Array[String] = Array.empty,
                 uriSans: Array[String] = Array.empty, otherSans: Array[String] = Array.empty,
                 excludeCnFromSans: Boolean = false)
