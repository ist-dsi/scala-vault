package pt.tecnico.dsi.vault.secretEngines.pki.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait Format extends EnumEntry with Lowercase
case object Format extends Enum[Format] with CirceEnum[Format] {
  case object Pem extends Format
  case object Der extends Format
  case object Pem_Bundle extends Format

  val values = findValues
}