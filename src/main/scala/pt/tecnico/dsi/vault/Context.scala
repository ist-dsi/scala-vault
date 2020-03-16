package pt.tecnico.dsi.vault

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Context {
  implicit def decoder[Data: Decoder]: Decoder[Context[Data]] = deriveDecoder(renaming.snakeCase, false, None)
}
case class Context[Data](requestId: String, leaseId: String, renewable: Boolean, leaseDuration: Int, data: Data,
                         wrapInfo: Option[String] = None, warnings: Option[List[String]] = None, auth: Option[Auth] = None)
