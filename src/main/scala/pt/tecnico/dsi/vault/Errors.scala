package pt.tecnico.dsi.vault

import io.circe.Decoder
import io.circe.generic.semiauto._

object Errors {
  implicit val decoder: Decoder[Errors] = deriveDecoder
}
case class Errors(errors: List[String])