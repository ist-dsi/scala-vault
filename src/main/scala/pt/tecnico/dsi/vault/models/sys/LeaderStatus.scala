package pt.tecnico.dsi.vault.models.sys

import io.circe.generic.extras.ConfiguredJsonCodec
import org.http4s.Uri
import org.http4s.circe.{decodeUri, encodeUri}

@ConfiguredJsonCodec
case class LeaderStatus(haEnabled: Boolean, isSelf: Boolean, leaderAddress: Uri, leaderClusterAddress: Uri,
                        performanceStandby: Boolean, performanceStandbyLastRemoteWal: Int)