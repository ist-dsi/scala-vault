package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import io.circe.{Decoder, HCursor}

object Entity {
  implicit val decoder: Decoder[Entity] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    name <- cursor.get[String]("name")
    creationTime <- cursor.get[OffsetDateTime]("creation_time")
    lastUpdateTime <- cursor.get[OffsetDateTime]("last_update_time")
    policies <- cursor.getOrElse[List[String]]("policies")(List.empty)
    aliases <- cursor.getOrElse[List[EntityAlias]]("aliases")(List.empty)
    groups <- cursor.getOrElse[List[String]]("group_ids")(List.empty)
    directGroups <- cursor.getOrElse[List[String]]("direct_group_ids")(List.empty)
    inheritedGroups <- cursor.getOrElse[List[String]]("inherited_group_ids")(List.empty)
    namespaceId <- cursor.get[String]("namespace_id")
    disabled <- cursor.getOrElse[Boolean]("disabled")(false)
    metadata <- cursor.getOrElse[Map[String, String]]("metadata")(Map.empty)
  } yield Entity(id, name, creationTime, lastUpdateTime, policies, aliases, groups, directGroups, inheritedGroups, namespaceId, disabled, metadata)
}
case class Entity(
  id: String,
  name: String,
  creationTime: OffsetDateTime,
  lastUpdateTime: OffsetDateTime,
  policies: List[String] = List.empty,
  aliases: List[EntityAlias] = List.empty,
  groups: List[String] = List.empty,
  directGroups: List[String] = List.empty,
  inheritedGroups: List[String] = List.empty,
  namespaceId: String,
  disabled: Boolean = false,
  metadata: Map[String, String] = Map.empty,
) extends Base