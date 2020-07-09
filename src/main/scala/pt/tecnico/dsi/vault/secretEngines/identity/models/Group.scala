package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}

object Group {
  sealed trait Type extends EnumEntry
  case object Type extends Enum[Type] {
    implicit val circeEncoder: Encoder[Type] = Circe.encoderLowercase(this)
    implicit val circeDecoder: Decoder[Type] = Circe.decoderLowercaseOnly(this)

    case object Internal extends Type
    case object External extends Type

    val values = findValues
  }

  implicit val decoder: Decoder[Group] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    name <- cursor.get[String]("name")
    creationTime <- cursor.get[OffsetDateTime]("creation_time")
    lastUpdateTime <- cursor.get[OffsetDateTime]("last_update_time")
    tpe <- cursor.get[Group.Type]("type")
    policies <- cursor.getOrElse[List[String]]("policies")(List.empty)
    alias <- cursor.get[JsonObject]("alias").flatMap { obj =>
      if (obj.isEmpty) Right(Option.empty[GroupAlias])
      else GroupAlias.decoder.decodeJson(Json.fromJsonObject(obj)).map(Some(_))
    }
    members <- cursor.getOrElse[List[String]]("member_entity_ids")(List.empty)
    subgroups <- cursor.getOrElse[List[String]]("member_group_ids")(List.empty)
    parentGroups <- cursor.getOrElse[List[String]]("parent_group_ids")(List.empty)
    namespaceId <- cursor.get[String]("namespace_id")
    modifyIndex <- cursor.get[Int]("modify_index")
    metadata <- cursor.getOrElse[Map[String, String]]("metadata")(Map.empty)
  } yield Group(id, name, creationTime, lastUpdateTime, tpe, policies, alias, members, subgroups, parentGroups, namespaceId, modifyIndex, metadata)
}
case class Group(
  id: String,
  name: String,
  creationTime: OffsetDateTime,
  lastUpdateTime: OffsetDateTime,
  `type`: Group.Type,
  policies: List[String] = List.empty,
  alias: Option[GroupAlias] = None,
  members: List[String] = List.empty,
  subgroups: List[String] = List.empty,
  parentGroups: List[String] = List.empty,
  namespaceId: String,
  modifyIndex: Int,
  metadata: Map[String, String] = Map.empty,
) extends Base