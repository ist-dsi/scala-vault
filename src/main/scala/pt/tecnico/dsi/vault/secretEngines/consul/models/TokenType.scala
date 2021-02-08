package pt.tecnico.dsi.vault.secretEngines.consul.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait TokenType extends EnumEntry with Lowercase

case object TokenType extends Enum[TokenType] with CirceEnum[TokenType] {
  case object Client extends TokenType
  case object Management extends TokenType

  val values = findValues
}