package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.generic.extras.semiauto.{deriveEnumerationEncoder, deriveEnumerationDecoder}

object Type {
  // A little ugly :(
  implicit val encoder = deriveEnumerationEncoder[Type].mapJson(_.mapString(_.toLowerCase))
  implicit val decoder = deriveEnumerationDecoder[Type].prepare(_.withFocus(_.mapString(_.capitalize)))
}

sealed trait Type
/** The private key will be returned in the response. */
case object Exported extends Type
/** Te private key will not be returned and cannot be retrieved later. */
case object Internal extends Type