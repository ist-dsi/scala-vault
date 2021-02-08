package pt.tecnico.dsi.vault.secretEngines.pki.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.secretEngines.pki.models.KeySettings.{Format, Type}

object KeySettings {
  implicit val codec: Codec.AsObject[KeySettings] = deriveCodec(renaming.snakeCase)
  
  sealed trait Type extends EnumEntry with Lowercase
  case object Type extends Enum[Type] with CirceEnum[Type] {
    case object RSA extends Type
    case object EC extends Type
    
    val values = findValues
  }
  
  sealed trait Format extends EnumEntry with Lowercase
  case object Format extends Enum[Format] with CirceEnum[Format] {
    case object Der extends Format
    case object Pkcs8 extends Format
    
    val values = findValues
  }
}

case class KeySettings(keyType: KeySettings.Type = Type.RSA, keyBits: Int = 2048, privateKeyFormat: KeySettings.Format = Format.Der)