package pt.tecnico.dsi.vault.secretEngines.identity

import cats.effect.Concurrent
import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveEncoder, renaming}
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST}
import org.http4s.Status.{NoContent, Ok}
import pt.tecnico.dsi.vault.{Context, DSL}
import pt.tecnico.dsi.vault.secretEngines.identity.models.*
import pt.tecnico.dsi.vault.secretEngines.identity.AliasCRUD.*

object AliasCRUD {
  case class AliasCreate(name: String, canonicalId: String, mountAccessor: String, id: Option[String] = None)
  implicit val encoder: Encoder[AliasCreate] = deriveEncoder(renaming.snakeCase)
}
/** @define name */
abstract class AliasCRUD[F[_]: Concurrent: Client, T <: Alias: Decoder](basePath: String, baseUri: Uri, baseName: String)(implicit token: Header.Raw) {
  private val dsl = new DSL[F] {}
  import dsl.*

  val path: String = s"$basePath/$baseName-alias"
  val uri: Uri = baseUri / s"$baseName-alias"

  /** List $name aliases by their identifiers. */
  val list: F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

  /**
    * Gets the $name alias with the given `id`.
    *
    * @param id the id of the $name alias.
    * @return if a $name alias with `id` exists a `Some` will be returned. `None` otherwise.
    */
  def get(id: String): F[Option[T]] = executeOptionWithContextData(GET(uri / "id" / id, token))
  /**
    * Gets the $name alias with the given `id`.
    *
    * @param id the id of the $name alias.
    */
  def apply(id: String): F[T] = executeWithContextData(GET(uri / "id" / id, token))

  protected def computeAliasId(name: String, canonicalId: String, mountAccessor: String): F[String]

  /**
    * Creates a new alias for a $name.
    *
    * @note this method it not idempotent. If the alias already exists Vault will return NoContent.
    *       Unfortunately there is nothing we can do, since we don't know the existing alias id, nor can we update the alias via the name.
    * @param name name for the $name alias.
    * @param canonicalId $name ID of to which this alias belongs to.
    * @param mountAccessor mount accessor which this alias belongs to. Can be consulted with:
    * {{{
    *   vaultClient.sys.auth.list().map { mountedAuths => mountedAuths(s"\$path/").accessor }
    * }}}
    * @return the id of the created alias.
    */
  def create(name: String, canonicalId: String, mountAccessor: String): F[String] =
    genericExecute(POST(AliasCreate(name, canonicalId, mountAccessor, None), uri, token)) {
      case Ok(response) =>
        implicit val d: Decoder[String] = Decoder.decodeString.at("id")
        response.as[Context[String]].map(_.data)
      case NoContent(_) =>computeAliasId(name, canonicalId, mountAccessor)
    }

  /**
    * Updates an existing $name alias.
    * @param id ID of the $name alias.
    * @param name name of the $name alias.
    * @param canonicalId $name ID of to which this alias belongs to.
    * @param mountAccessor mount accessor which this alias belongs to.
    */
  def update(id: String, name: String, canonicalId: String, mountAccessor: String): F[Unit] =
    execute(POST(AliasCreate(name, canonicalId, mountAccessor, Some(id)), uri / "id" / id, token))

  /**
    * Deletes a $name alias.
    * @param id identifier of the $name alias.
    */
  def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
}

/**
  * @define name
  * @define namePlural
  */
abstract class BaseEndpoints[F[_]: Concurrent: Client, T <: Base: Decoder](basePath: String, baseUri: Uri, name: String)(implicit token: Header.Raw) {
  private val dsl = new DSL[F] {}
  import dsl.*

  val path: String = s"$basePath/$name"
  val uri: Uri = baseUri / s"$name"

  /** Lists $namePlural by their names. */
  val list: F[List[String]] = executeWithContextKeys(LIST(uri / "name", token))
  /** Lists $namePlural by their identifiers. */
  val listById: F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

  /** Gets the $name with the given `name`. */
  def get(name: String): F[Option[T]] = executeOptionWithContextData(GET(uri / "name" / name, token))
  /** Gets the $name with the given `name`, assuming it exists. */
  def apply(name: String): F[T] = executeWithContextData(GET(uri / "name" / name, token))
  /** Gets the $name with the given `id`. */
  def getById(id: String): F[Option[T]] = executeOptionWithContextData(GET(uri / "id" / id, token))
  /** Gets the $name with the given `id`, assuming it exists. */
  def applyById(id: String): F[T] = executeWithContextData(GET(uri / "id" / id, token))

  protected def create(name: String): F[T]

  /** Gets the $name named `name`, creating one if one does not exist. */
  def findOrCreate(name: String): F[T] =
    get(name).flatMap {
      case Some(value) => value.pure[F]
      case None => create(name)
    }

  /** Deletes the $name with `name` and all its associated aliases. */
  def delete(name: String): F[Unit] = execute(DELETE(uri / "name" / name, token))
  /** Deletes the $name with `id` and all its associated aliases. */
  def deleteById(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
}

final class Identity[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header.Raw) { self =>
  private val dsl = new DSL[F] {}
  import dsl.*

  /**
    * @define name entity
    * @define namePlural entities
    */
  object entity extends BaseEndpoints[F, Entity](path, uri, "entity") {
    // The endpoint POST /identity/entity is not here on purpose. It accepts the same arguments as the create below plus an id.
    // According to the documentation it creates or updates an Entity. However if you pass it an ID which does not exist it won't
    // create an entity with that ID but rather return an error. Thus it becomes awkward to use.

    /**
      * Create or update an entity by a given name.
      *
      * @param name name of the entity.
      * @param policies policies to be tied to the entity.
      * @param disabled whether the entity is disabled. Disabled entities' associated tokens cannot be used, but are not revoked.
      * @param metadata metadata to be associated with the entity.
      * @return the entity id.
      */
    def create(name: String, policies: List[String] = List.empty, disabled: Boolean = false, metadata: Map[String, String] = Map.empty): F[Entity] = {
      val body = Map(
        "policies" -> policies.asJson,
        "disabled" -> disabled.asJson,
        "metadata" -> metadata.asJson,
      )
      // If a new entity is created an Ok will be returned with the body containing the entity id.
      // If an entity with `name` already exists a NoContent will be returned.
      genericExecute(POST(body, uri / "name" / name, token)) {
        case Ok(response) =>
          implicit val d: Decoder[String] = Decoder.decodeString.at("id")
          response.as[Context[String]].flatMap(c => applyById(c.data))
        case NoContent(_) => apply(name)
      }
    }

    /**
      * Updates an existing entity.
      *
      * @param id identifier of the entity.
      * @param name name of the entity.
      * @param policies policies to be tied to the entity.
      * @param disabled whether the entity is disabled. Disabled entities' associated tokens cannot be used, but are not revoked.
      * @param metadata metadata to be associated with the entity.
      * @return the entity id.
      */
    def update(id: String, name: String, policies: List[String] = List.empty, disabled: Boolean = false, metadata: Map[String, String] = Map.empty): F[Unit] = {
      val body = Map(
        "name" -> name.asJson,
        "policies" -> policies.asJson,
        "disabled" -> disabled.asJson,
        "metadata" -> metadata.asJson,
      )
      execute(POST(body, uri / "id" / id, token))
    }
    /**
      * Updates an entity using `entity` to fetch the values for `id`, `name`, `policies`, `disabled`, and `metadata`.
      * Any other field will not be used.
      * @param entity the entity from which to fetch the values.
      */
    def update(entity: Entity): F[Unit] = update(entity.id, entity.name, entity.policies, entity.disabled, entity.metadata)

    override protected def create(name: String): F[Entity] = create(name, List.empty)

    /**
      * Appends `policies` to an entity named `name`. If no entity exists with that name a new entity will be created.
      *
      * @param name name of the entity.
      * @param policies the list of policies to append to the entity.
      * @return the entity id to which the policies were appended to.
      */
    def addPolicies(name: String, policies: String*): F[String] = findOrCreate(name).flatMap { entity =>
      update(entity.copy(policies = (entity.policies ++ policies).distinct)).map(_ => entity.id)
    }

    /**
      * Deletes all entities provided.
      * @param ids list of entity identifiers to delete.
      */
    def batchDelete(ids: List[String]): F[Unit] = execute(POST(Map("entity_ids" -> ids), self.uri / "batch-delete", token))

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

  /** @define name entity */
  object entityAlias extends AliasCRUD[F, EntityAlias](path, uri, "entity") {
    override protected def computeAliasId(name: String, canonicalId: String, mountAccessor: String): F[String] =
      entity.applyById(canonicalId).map { entity =>
        entity.aliases.collectFirst {
          case alias if alias.name == name && alias.mountAccessor == mountAccessor => alias.id
        }.get // We now the alias must exist
      }
  }

  /**
    * @define name group
    * @define namePlural groups
    */
  object group extends BaseEndpoints[F, Group](path, uri, "group") {
    // The endpoint POST /identity/group is not here on purpose. It accepts the same arguments as the create below plus an id.
    // According to the documentation it creates or updates a Group. However if you pass it an ID which does not exist it won't
    // create a Group with that ID but rather return an error. Thus it becomes awkward to use.

    /**
      * Creates or updates a Group.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities.
      *
      * @param name name of the group.
      * @param policies policies to be tied to the group.
      * @param members entity IDs to be assigned as group members.
      * @param subgroups group IDs to be assigned as group members.
      * @param `type` type of the group.
      * @param metadata metadata to be associated with the group.
      * @return the id of the created group.
      */
    def create(name: String, policies: List[String] = List.empty, members: List[String] = List.empty, subgroups: List[String] = List.empty,
               `type`: Group.Type = Group.Type.Internal, metadata: Map[String, String] = Map.empty): F[Group] = {
      val body = Map(
        "policies" -> policies.asJson,
        "member_entity_ids" -> members.asJson,
        "member_group_ids" -> subgroups.asJson,
        "type" -> `type`.asJson,
        "metadata" -> metadata.asJson,
      )
      // If a new group is created an Ok will be returned with the body containing the entity id.
      // If a group with `name` already exists a NoContent will be returned.
      genericExecute(POST(body, uri / "name" / name, token)) {
        case Ok(response) =>
          implicit val d: Decoder[String] = Decoder.decodeString.at("id")
          response.as[Context[String]].flatMap(c => applyById(c.data))
        case NoContent(_) => apply(name)
      }
    }

    /**
      * Updates an existing group.
      * @param id identifier of the group.
      * @param name name of the group.
      * @param policies policies to be tied to the group.
      * @param members entity IDs to be assigned as group members.
      * @param subgroups group IDs to be assigned as group members.
      * @param `type` type of the group.
      * @param metadata metadata to be associated with the group.
      * @return
      */
    def update(id: String, name: String, policies: List[String] = List.empty, members: List[String] = List.empty, subgroups: List[String] = List.empty,
               `type`: Group.Type = Group.Type.Internal, metadata: Map[String, String] = Map.empty): F[Unit] = {
      val body = Map(
        "name" -> name.asJson,
        "policies" -> policies.asJson,
        "member_entity_ids" -> members.asJson,
        "member_group_ids" -> subgroups.asJson,
        "type" -> `type`.asJson,
        "metadata" -> metadata.asJson,
      )
      execute(POST(body, uri / "id" / id, token))
    }
    /**
      * Updates `group`.
      * @param group the group to update.
      */
    def update(group: Group): F[Unit] =
      update(group.id, group.name, group.policies, group.members, group.subgroups, group.`type`, group.metadata)

    override protected def create(name: String): F[Group] = create(name, List.empty)

    /**
      * Appends `policies` to a group named `name`. If no group exists with that name a new group will be created.
      *
      * @param name name of the group.
      * @param policies the list of policies to append to the group.
      * @return the group id to which the policies were appended to.
      */
    def addPolicies(name: String, policies: String*): F[String] = findOrCreate(name).flatMap { group =>
      update(group.copy(policies = (group.policies ++ policies).distinct)).map(_ => group.id)
    }

    /**
      * Appends `members` to a group named `name`. If no group exists with that name a new group will be created.
      *
      * @param name name of the group.
      * @param members the list of entity IDs to append to the group.
      * @return the group id to which the members were appended to.
      */
    def addMembers(name: String, members: String*): F[String] = findOrCreate(name).flatMap { group =>
      update(group.copy(members = (group.members ++ members).distinct)).map(_ => group.id)
    }

    /**
      * Appends `subgroups` to a group named `name`. If no group exists with that name a new group will be created.
      *
      * @param name name of the group.
      * @param subgroupsIDs the list of group Ids to append to the subgroups of group.
      * @return the group id to which the subgroups were appended to.
      */
    def addSubgroups(name: String, subgroupsIDs: String*): F[String] = findOrCreate(name).flatMap { group =>
      update(group.copy(subgroups = (group.subgroups ++ subgroupsIDs).distinct)).map(_ => group.id)
    }
  }

  /** @define name group */
  object groupAlias extends AliasCRUD[F, GroupAlias](path, uri, "group") {
    override protected def computeAliasId(name: String, canonicalId: String, mountAccessor: String): F[String] = {
      // We know the alias is defined
      group.applyById(canonicalId).map(_.alias.get.id)
    }
  }
}