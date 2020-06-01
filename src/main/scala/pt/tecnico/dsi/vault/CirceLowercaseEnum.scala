package pt.tecnico.dsi.vault

import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

trait CirceLowercaseEnum[A <: EnumEntry] { this: Enum[A] =>
  /** Implicit Encoder for this enum, which encodes the entries in lowercase. */
  implicit val encoder: Encoder[A] = Circe.encoderLowercase(this)

  /** Implicit Decoder for this enum, which decodes the entries in lowercase. */
  implicit val decoder: Decoder[A] = Circe.decoderLowercaseOnly(this)
}