package pt.tecnico.dsi.vault.secretEngines.pki.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait Type extends EnumEntry with Lowercase
case object Type extends Enum[Type] with CirceEnum[Type] {
  /** The private key will be returned in the response. */
  case object Exported extends Type
  /** Te private key will not be returned and cannot be retrieved later. */
  case object Internal extends Type

  val values = findValues
}
