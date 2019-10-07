package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder

object SealStatus {
  implicit val decoder: Decoder[SealStatus] =
    Decoder.forProduct9("type", "sealed", "t", "n", "progress",
      "version", "nonce", "cluster_name", "cluster_id")(SealStatus.apply)
}
case class SealStatus(`type`: String, `sealed`: Boolean,
                      secretThreshold: Int, secretShares: Int,
                      progress: Int, version: String, nonce: String,
                      clusterName: Option[String], clusterId: Option[String])