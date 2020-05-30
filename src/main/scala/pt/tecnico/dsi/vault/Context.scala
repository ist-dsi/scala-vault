package pt.tecnico.dsi.vault

import io.circe.{Decoder, Json}
import io.circe.derivation.{deriveDecoder, renaming}

object Context {
  implicit def decoder[Data: Decoder]: Decoder[Context[Data]] = deriveDecoder(renaming.snakeCase)
}
case class Context[Data](renewable: Boolean, leaseId: String, leaseDuration: Int, data: Data,
                         auth: Option[Auth] = None, metadata: Option[Json],
                         warnings: Option[List[String]] = None, wrapInfo: Option[String] = None)
