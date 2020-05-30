package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object SealStatus {
  implicit val decoder: Decoder[SealStatus] = deriveDecoder {
    case "secretThreshold" => "t"
    case "secretShares" => "n"
    case s => renaming.snakeCase(s)
  }
}
case class SealStatus(`type`: String, `sealed`: Boolean,
                      secretThreshold: Int, secretShares: Int,
                      progress: Int, version: String, nonce: String,
                      clusterName: Option[String], clusterId: Option[String])