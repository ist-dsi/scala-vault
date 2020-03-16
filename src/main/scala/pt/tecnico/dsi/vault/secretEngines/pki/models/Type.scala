package pt.tecnico.dsi.vault.secretEngines.pki.models

import enumeratum.{EnumEntry, Enum}
import pt.tecnico.dsi.vault.CirceLowercaseEnum

sealed trait Type extends EnumEntry
case object Type extends Enum[Type] with CirceLowercaseEnum[Type] {
  /** The private key will be returned in the response. */
  case object Exported extends Type
  /** Te private key will not be returned and cannot be retrieved later. */
  case object Internal extends Type

  val values = findValues
}
