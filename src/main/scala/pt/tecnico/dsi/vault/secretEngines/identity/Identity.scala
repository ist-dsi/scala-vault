package pt.tecnico.dsi.vault.secretEngines.identity

import cats.effect.Sync
import io.circe.syntax._
import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.secretEngines.identity.models.{Entity, EntityAlias, Group, GroupAlias}
import pt.tecnico.dsi.vault.secretEngines.identity.Identity.{Alias, EntityCreate, GroupCreate}

object Identity {
  implicit val encoderAlias: Encoder[Alias] = deriveEncoder
  case class Alias(name: String, canonicalId: String, mountAccessor: String, id: Option[String] = None)

  implicit val encoderGroupCreate: Encoder[GroupCreate] = Encoder.forProduct7("id", "name", "type", "policies", "member_entity_ids", "member_group_ids",
    "metadata")(g => (g.id, g.name, g.`type`, g.policies, g.members, g.memberGroups, g.metadata))
  case class GroupCreate(
    name: String,
    `type`: Group.Type = Group.Type.Internal,
    policies: List[String] = List.empty,
    members: List[String] = List.empty,
    memberGroups: List[String] = List.empty,
    metadata: Map[String, String] = Map.empty,
    id: Option[String] = None,
  )

  implicit val encoderEntityCreate: Encoder[EntityCreate] = deriveEncoder
  case class EntityCreate(
    name: String,
    policies: List[String] = List.empty,
    disabled: Boolean = false,
    metadata: Map[String, String] = Map.empty,
    id: Option[String] = None,
  )
}
final class Identity[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  object entity {
    val path: String = s"${self.path}/entity"
    val uri: Uri = self.uri / "entity"

    /** List of available entities by their identifiers. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

    /**
      * Gets the Entity with the given id.
      *
      * @param id the id of the entity.
      * @return if an Entity with `id` exists a `Some` will be returned. `None` otherwise.
      */
    def get(id: String): F[Option[Entity]] = executeOptionWithContextData(GET(uri / "id" / id, token))
    /**
      * Gets the Entity with the given id.
      *
      * @param id the id of the entity.
      */
    def apply(id: String): F[Entity] = executeWithContextData(GET(uri / "id" / id, token))

    /**
      * Creates or updates an Entity.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(entity: EntityCreate): F[Unit] = execute(POST(entity, uri, token))

    /** Updates an existing entity. */
    def update(id: String, entity: EntityCreate): F[Unit] = execute(POST(entity.asJson.mapObject(_.remove("id")), uri / "id" / id, token))

    /**
      * Deletes an entity and all its associated aliases.
      * @param id the entity id to delete.
      */
    def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))

    /**
      * Deletes all entities provided.
      * @param ids list of entity identifiers to delete.
      */
    def batchDelete(ids: List[String]): F[Unit] = execute(POST(Map("entity_ids" -> ids), self.uri / "batch-delete", token))

    /** Lists of available entities by their names. */
    def listByName(): F[List[String]] = executeWithContextKeys(LIST(uri / "name", token))

    /**
      * Gets the Entity with the given name.
      *
      * @param name name of the entity.
      * @return if an Entity with `name` exists a `Some` will be returned. `None` otherwise.
      */
    def getByName(name: String): F[Option[Entity]] = executeOptionWithContextData(GET(uri / "name" / name, token))

    /**
      * Create or update an entity by a given name.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def createByName(entity: EntityCreate): F[Unit] = execute(POST(entity.asJson.mapObject(_.remove("id")), uri / "name" / entity.name, token))

    /** Updates an existing entity. */
    def updateByName(id: String, entity: Entity): F[Unit] = execute(POST(entity, uri / "id" / id, token))

    /**
      * Deletes an entity and all its associated aliases, given the entity name.
      * @param name name of the entity.
      */
    def deleteByName(name: String): F[Unit] = execute(DELETE(uri / "name" / name, token))

    /**
      * Merges many entities into one entity.
      * @param from Entity IDs which needs to get merged.
      * @param to Entity ID into which all the other entities need to get merged.
      * @param force Setting this will follow the 'mine' strategy for merging MFA secrets. If there are secrets of the same type both in
      *              entities that are merged from and in entity into which all others are getting merged, secrets in the destination
      *              will be unaltered. If not set, this API will throw an error containing all the conflicts.
      */
    def merge(from: List[String], to: String, force: Boolean = false): F[Unit] =
      execute(POST(Map("from_entity_ids" -> from.asJson, "to_entity_id" -> to.asJson, "force" -> force.asJson), uri / "merge", token))
  }

  object entityAlias {
    val path: String = s"${self.path}/entity-alias"
    val uri: Uri = self.uri / "entity-alias"

    /** List of available entity aliases by their identifiers. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

    /**
      * Gets the entity alias with the given `id`.
      *
      * @param id the id of the entity alias.
      * @return if an entity alias with `id` exists a `Some` will be returned. `None` otherwise.
      */
    def get(id: String): F[Option[EntityAlias]] = executeOptionWithContextData(GET(uri / "id" / id, token))
    /**
      * Gets the entity alias with the given `id`.
      *
      * @param id the id of the entity alias.
      */
    def apply(id: String): F[EntityAlias] = executeWithContextData(GET(uri / "id" / id, token))

    /**
      * Creates a new alias for an entity.
      * @param name name of the alias. Name should be the identifier of the client in the authentication source.
      *             For example, if the alias belongs to userpass backend, the name should be a valid username within userpass backend.
      *             If alias belongs to GitHub, it should be the GitHub username.
      * @param canonicalId Entity ID to which this alias belongs to.
      * @param mountAccessor mount accessor which this alias belongs to.
      * @param id ID of the entity alias. If set, updates the corresponding entity alias.
      */
    def create(name: String, canonicalId: String, mountAccessor: String, id: Option[String] = None): F[Unit] =
      execute(POST(Alias(name, canonicalId, mountAccessor, id), uri, token))

    /**
      * Updates an existing entity alias.
      *
      * @param id ID of the entity alias.
      * @param name name of the alias. Name should be the identifier of the client in the authentication source.
      *             For example, if the alias belongs to userpass backend, the name should be a valid username within userpass backend.
      *             If alias belongs to GitHub, it should be the GitHub username.
      * @param canonicalId entity ID to which this alias belongs to.
      * @param mountAccessor mount accessor which this alias belongs to.
      */
    def update(id: String, name: String, canonicalId: String, mountAccessor: String): F[Unit] =
      execute(POST(Alias(name, canonicalId, mountAccessor, Some(id)), uri / "id" / id, token))

    /**
      * Deletes an alias from its corresponding entity.
      * @param id Identifier of the entity alias.
      */
    def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
  }

  object group {
    val path: String = s"${self.path}/group"
    val uri: Uri = self.uri / "group"

    /** List of available entities by their identifiers. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

    /**
      * Gets the Entity with the given id.
      *
      * @param id the id of the entity.
      * @return if an Entity with `id` exists a `Some` will be returned. `None` otherwise.
      */
    def get(id: String): F[Option[Group]] = executeOptionWithContextData(GET(uri / "id" / id, token))
    /**
      * Gets the Entity with the given id.
      *
      * @param id the id of the entity.
      */
    def apply(id: String): F[Group] = executeWithContextData(GET(uri / "id" / id, token))

    /**
      * Creates or updates a Group.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(group: GroupCreate): F[Unit] = execute(POST(group, uri, token))

    /** Updates an existing group. */
    def update(id: String, group: GroupCreate): F[Unit] = execute(POST(group.asJson.mapObject(_.remove("id")), uri / "id" / id, token))

    /**
      * Deletes an entity and all its associated aliases.
      * @param id the entity id to delete.
      */
    def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))

    /**
      * Deletes all entities provided.
      * @param ids list of entity identifiers to delete.
      */
    def batchDelete(ids: List[String]): F[Unit] = execute(POST(Map("entity_ids" -> ids), self.uri / "batch-delete", token))


    /** Lists of available entities by their names. */
    def listByName(): F[List[String]] = executeWithContextKeys(LIST(uri / "name", token))

    /**
      * Gets the Entity with the given name.
      *
      * @param name name of the entity.
      * @return if an Entity with `name` exists a `Some` will be returned. `None` otherwise.
      */
    def getByName(name: String): F[Option[Group]] = executeOptionWithContextData(GET(uri / "name" / name, token))

    /**
      * Create or update an entity by a given name.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def createByName(entity: Entity): F[Unit] = execute(POST(entity.asJson.mapObject(_.remove("id")), uri / "name" / entity.name, token))

    /** Updates an existing entity. */
    def updateByName(id: String, entity: Entity): F[Unit] = execute(POST(entity, uri / "id" / id, token))

    /**
      * Deletes an entity and all its associated aliases, given the entity name.
      * @param name name of the entity.
      */
    def deleteByName(name: String): F[Unit] = execute(DELETE(uri / "name" / name, token))

    /**
      * Merges many entities into one entity.
      * @param from Entity IDs which needs to get merged.
      * @param to Entity ID into which all the other entities need to get merged.
      * @param force Setting this will follow the 'mine' strategy for merging MFA secrets. If there are secrets of the same type both in
      *              entities that are merged from and in entity into which all others are getting merged, secrets in the destination
      *              will be unaltered. If not set, this API will throw an error containing all the conflicts.
      */
    def merge(from: List[String], to: String, force: Boolean = false): F[Unit] =
      execute(POST(Map("from_entity_ids" -> from.asJson, "to_entity_id" -> to.asJson, "force" -> force.asJson), uri / "merge", token))
  }

  object groupAlias {
    val path: String = s"${self.path}/group-alias"
    val uri: Uri = self.uri / "group-alias"

    /** List of available group aliases by their identifiers. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

    /**
      * Gets the group alias with the given `id`.
      *
      * @param id the id of the group alias.
      * @return if a group alias with `id` exists a `Some` will be returned. `None` otherwise.
      */
    def get(id: String): F[Option[GroupAlias]] = executeOptionWithContextData(GET(uri / "id" / id, token))
    /**
      * Gets the group alias with the given `id`.
      *
      * @param id the id of the group alias.
      */
    def apply(id: String): F[GroupAlias] = executeWithContextData(GET(uri / "id" / id, token))

    /**
      * Creates a new alias for a group.
      * @param name name for the group alias.
      * @param canonicalId group ID of to which this alias belongs to.
      * @param mountAccessor mount accessor which this alias belongs to.
      * @param id ID of the group alias. If set, updates the corresponding existing group alias.
      */
    def create(name: String, canonicalId: String, mountAccessor: String, id: Option[String] = None): F[Unit] =
      execute(POST(Alias(name, canonicalId, mountAccessor, id), uri, token))

    /**
      * Updates an existing group alias.
      * @param id ID of the group alias.
      * @param name name of the group alias.
      * @param canonicalId group ID of to which this alias belongs to.
      * @param mountAccessor mount accessor which this alias belongs to.
      */
    def update(id: String, name: String, canonicalId: String, mountAccessor: String): F[Unit] =
      execute(POST(Alias(name, canonicalId, mountAccessor, Some(id)), uri / "id" / id, token))

    /**
      * Deletes a group alias.
      * @param id identifier of the group alias.
      */
    def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
  }
}