package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.{Decoder, Encoder}

object KeySettings {
  implicit val encoder = deriveEncoder[KeySettings](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[KeySettings](renaming.snakeCase, false, None)

  object Type {
    implicit val encoder: Encoder[Type] = deriveEnumerationEncoder[Type].mapJson(_.mapString(_.toLowerCase))
    implicit val decoder: Decoder[Type] = deriveEnumerationDecoder[Type].prepare(_.withFocus(_.mapString(_.toUpperCase)))
  }
  sealed trait Type
  case object RSA extends Type
  case object EC extends Type

  object Format {
    implicit val encoder: Encoder[Format] = deriveEnumerationEncoder[Format].mapJson(_.mapString(_.toLowerCase))
    implicit val decoder: Decoder[Format] = deriveEnumerationDecoder[Format].prepare(_.withFocus(_.mapString(_.capitalize)))
  }
  sealed trait Format
  case object Der extends Format
  case object Pkcs8 extends Format
}

case class KeySettings(keyType: KeySettings.Type = KeySettings.RSA, keyBits: Int = 2048, privateKeyFormat: KeySettings.Format = KeySettings.Der)