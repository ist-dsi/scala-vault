package pt.tecnico.dsi.vault.secretEngines.pki.models

import enumeratum.{Enum, EnumEntry}
import pt.tecnico.dsi.vault.CirceLowercaseEnum

sealed trait Format extends EnumEntry
case object Format extends Enum[Format] with CirceLowercaseEnum[Format] {
  case object Pem extends Format
  case object Der extends Format
  case object Pem_Bundle extends Format

  val values = findValues
}