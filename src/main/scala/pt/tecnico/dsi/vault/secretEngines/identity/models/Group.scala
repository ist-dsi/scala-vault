package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Codec, Decoder, Encoder}

object Group {
  sealed trait Type extends EnumEntry
  case object Type extends Enum[Type] {
    implicit val circeEncoder: Encoder[Type] = Circe.encoderLowercase(this)
    implicit val circeDecoder: Decoder[Type] = Circe.decoderLowercaseOnly(this)

    case object Internal extends Type
    case object External extends Type

    val values = findValues
  }

  implicit val codec: Codec[Group] = Codec.forProduct13("id", "name", "creation_time", "last_updated_time", "type", "policies", "alias", "member_entity_ids",
    "member_group_ids", "parent_group_ids", "namespace_id", "modify_index", "metadata")(Group.apply)(g =>
    (g.id, g.name, g.creationTime, g.lastUpdateTime, g.`type`, g.policies, g.alias, g.members, g.subgroups, g.parentGroups, g.namespaceId, g.modifyIndex,
      g.metadata)
  )
}
case class Group(
  id: String,
  name: String,
  creationTime: OffsetDateTime,
  lastUpdateTime: OffsetDateTime,
  `type`: Group.Type = Group.Type.Internal,
  policies: List[String] = List.empty,
  alias: Option[Group] = None,
  members: List[String] = List.empty,
  subgroups: List[String] = List.empty,
  parentGroups: List[String] = List.empty,
  namespaceId: String,
  modifyIndex: Int,
  metadata: Map[String, String] = Map.empty,
)