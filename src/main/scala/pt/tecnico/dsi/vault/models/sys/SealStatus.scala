package pt.tecnico.dsi.vault.models.sys

import io.circe.generic.extras._

@ConfiguredJsonCodec
case class SealStatus(`type`: String, `sealed`: Boolean,
                      @JsonKey("t") secretThreshold: Int, @JsonKey("n") secretShares: Int,
                      progress: Int, version: String, nonce: String,
                      clusterName: Option[String], clusterId: Option[String])