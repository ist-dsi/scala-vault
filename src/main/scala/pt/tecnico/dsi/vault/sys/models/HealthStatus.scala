package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object HealthStatus {
  implicit val decoder: Decoder[HealthStatus] = deriveDecoder(renaming.snakeCase, false, None)
}

/**
  * @param initialized Whether the Vault server is Initialized.
  * @param `sealed` Whether the Vault server is Sealed.
  * @param standby Whether the Vault server is in Standby mode.
  * @param replicationPerformanceMode Verbose description of DR mode
  * @param replicationDrMode Verbose description of DR mode
  * @param serverTimeUtc Server time in Unix seconds, UTC
  * @param version Server Vault version
  * @param clusterName Server cluster name
  * @param clusterId Server cluster UUID
  */
case class HealthStatus(initialized: Boolean, `sealed`: Boolean, standby: Boolean, performanceStandby: Boolean,
                        replicationPerformanceMode: String, replicationDrMode: String,
                        serverTimeUtc: Int, version: String, clusterName: String, clusterId: String)