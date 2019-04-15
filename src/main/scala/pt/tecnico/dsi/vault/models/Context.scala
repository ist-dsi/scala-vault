package pt.tecnico.dsi.vault.models

import io.circe.{Decoder, Encoder}

object Context {
  implicit def encoder[Data](implicit dataEncoder: Encoder[Data]): Encoder[Context[Data]] =
    Encoder.forProduct8("request_id", "lease_id", "renewable", "lease_duration",
      "data", "wrap_info", "warnings", "auth")(c =>
      (c.requestId, c.leaseId, c.renewable, c.leaseDuration, c.data, c.wrapInfo, c.warnings, c.auth))

  implicit def decoder[Data](implicit dataDecoder: Decoder[Data]): Decoder[Context[Data]] =
    Decoder.forProduct8("request_id", "lease_id", "renewable", "lease_duration",
      "data", "wrap_info", "warnings", "auth")(Context.apply)
}
case class Context[Data](requestId: String, leaseId: String, renewable: Boolean, leaseDuration: Int, data: Data,
                         wrapInfo: Option[String] = None, warnings: Option[String] = None, auth: Option[Auth] = None)
