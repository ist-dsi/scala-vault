package pt.tecnico.dsi.vault

import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

trait CirceLowercaseEnum[A <: EnumEntry] { this: Enum[A] =>
  /**
    * Implicit Encoder for this enum
    */
  implicit val circeEncoder: Encoder[A] = Circe.encoderLowercase(this)

  /**
    * Implicit Decoder for this enum
    */
  implicit val circeDecoder: Decoder[A] = Circe.decoderLowercaseOnly(this)

}