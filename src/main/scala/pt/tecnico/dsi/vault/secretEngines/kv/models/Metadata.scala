package pt.tecnico.dsi.vault.secretEngines.kv.models

import java.time.OffsetDateTime
import io.circe.{Decoder, HCursor}

object Metadata {
  implicit val decoder: Decoder[Metadata] = (cursor: HCursor) => for {
    createdTime <- cursor.get[OffsetDateTime]("created_time")
    updatedTime <- cursor.get[OffsetDateTime]("updated_time")
    currentVersion <- cursor.get[Int]("current_version")
    oldestVersion <- cursor.get[Int]("oldest_version")
    incorrectVersions <- cursor.get[Map[Int, VersionMetadata]]("versions")
    versions = incorrectVersions.map { case (number, version) => (number, version.copy(version = number)) }
    configuration <- cursor.as[Configuration]
  } yield Metadata(createdTime, updatedTime, currentVersion, oldestVersion, versions, configuration)
}
case class Metadata(
  createdTime: OffsetDateTime, updatedTime: OffsetDateTime,
  currentVersion: Int, oldestVersion: Int,
  versions: Map[Int, VersionMetadata],
  configuration: Configuration,
)