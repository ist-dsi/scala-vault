package pt.tecnico.dsi.vault.secretEngines.consul.models

import enumeratum.{EnumEntry, Enum}
import pt.tecnico.dsi.vault.CirceLowercaseEnum

sealed trait TokenType extends EnumEntry

case object TokenType extends Enum[TokenType] with CirceLowercaseEnum[TokenType] {
  case object Client extends TokenType
  case object Management extends TokenType

  val values = findValues
}