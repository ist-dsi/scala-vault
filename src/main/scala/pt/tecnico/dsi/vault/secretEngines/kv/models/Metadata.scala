package pt.tecnico.dsi.vault.secretEngines.kv.models

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration
import io.circe.{Decoder, HCursor}
import pt.tecnico.dsi.vault.decoderFiniteDuration

object Metadata {
  implicit val decoder: Decoder[Metadata] = (c: HCursor) => for {
    createdTime <- c.get[OffsetDateTime]("created_time")
    updatedTime <- c.get[OffsetDateTime]("updated_time")
    currentVersion <- c.get[Int]("current_version")
    oldestVersion <- c.get[Int]("oldest_version")
    versions <- c.get[Map[Int, Version]]("versions")
    maxVersions <- c.get[Int]("max_versions")
    casRequired <- c.get[Boolean]("cas_required")
    deleteVersionAfter <- c.get[FiniteDuration]("delete_version_after")
  } yield Metadata(createdTime, updatedTime, currentVersion, oldestVersion, versions, Configuration(maxVersions, casRequired, deleteVersionAfter))
}
case class Metadata(
  createdTime: OffsetDateTime, updatedTime: OffsetDateTime,
  currentVersion: Int, oldestVersion: Int,
  versions: Map[Int, Version],
  configuration: Configuration,
)