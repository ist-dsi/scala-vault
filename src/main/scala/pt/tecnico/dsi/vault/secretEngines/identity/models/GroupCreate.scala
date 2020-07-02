package pt.tecnico.dsi.vault.secretEngines.identity.models

import io.circe.Encoder

object GroupCreate {
  implicit val encoder: Encoder[GroupCreate] = Encoder.forProduct7("id", "name", "type", "policies", "member_entity_ids", "member_group_ids",
    "metadata")(g => (g.id, g.name, g.`type`, g.policies, g.members, g.memberGroups, g.metadata))
}
case class GroupCreate(
  name: String,
  `type`: Group.Type = Group.Type.Internal,
  policies: List[String] = List.empty,
  members: List[String] = List.empty,
  memberGroups: List[String] = List.empty,
  metadata: Map[String, String] = Map.empty,
  id: Option[String] = None,
)
