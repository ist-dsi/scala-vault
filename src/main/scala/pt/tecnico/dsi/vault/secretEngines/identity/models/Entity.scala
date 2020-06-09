package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import io.circe.Codec

object Entity {
  implicit val codec: Codec[Entity] = Codec.forProduct12("id", "name", "creation_time", "last_updated_time", "policies", "aliases", "group_ids",
    "direct_group_ids", "inherited_group_ids", "namespace_id", "disabled", "metadata")(Entity.apply)(e =>
    (e.id, e.name, e.creationTime, e.lastUpdateTime, e.policies, e.aliases, e.groups, e.directGroups, e.inheritedGroups, e.namespaceId, e.disabled,
      e.metadata)
  )
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
)