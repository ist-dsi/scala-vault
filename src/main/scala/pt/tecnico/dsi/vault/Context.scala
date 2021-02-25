package pt.tecnico.dsi.vault

import scala.annotation.nowarn
import io.circe.{Decoder, Json}
import io.circe.derivation.{deriveDecoder, renaming}

object Context {
  // nowarn because of a false negative from the compiler. The Decoder is being inside the macro deriveDecoder.
  implicit def decoder[Data](implicit @nowarn d: Decoder[Data]): Decoder[Context[Data]] = deriveDecoder(renaming.snakeCase)
}
case class Context[Data](renewable: Boolean, leaseId: String, leaseDuration: Int, data: Data,
                         auth: Option[Auth] = None, metadata: Option[Json],
                         warnings: Option[List[String]] = None, wrapInfo: Option[String] = None)
