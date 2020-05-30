package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import org.http4s.Uri
import org.http4s.circe.decodeUri

object LeaderStatus {
  implicit val decoder: Decoder[LeaderStatus] = deriveDecoder(renaming.snakeCase)
}
case class LeaderStatus(haEnabled: Boolean, isSelf: Boolean, leaderAddress: Uri, leaderClusterAddress: Uri,
                        performanceStandby: Boolean, performanceStandbyLastRemoteWal: Int)