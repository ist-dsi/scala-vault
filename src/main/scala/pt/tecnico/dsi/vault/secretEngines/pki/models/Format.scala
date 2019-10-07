package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.generic.extras.semiauto.{deriveEnumerationEncoder, deriveEnumerationDecoder}

object Format {
  // A little ugly :(
  implicit val encoder = deriveEnumerationEncoder[Format].mapJson(_.mapString(_.toLowerCase))
  implicit val decoder = deriveEnumerationDecoder[Format].prepare(_.withFocus(_.mapString { str =>
    str.split('_').map(_.capitalize).mkString("_")
  }))
}

sealed trait Format
case object Pem extends Format
case object Der extends Format
case object Pem_Bundle extends Format