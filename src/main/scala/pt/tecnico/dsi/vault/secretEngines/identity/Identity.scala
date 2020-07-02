package pt.tecnico.dsi.vault.secretEngines.identity

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.syntax._
import io.circe.Decoder
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST}
import org.http4s.Status.{NoContent, Ok}
import pt.tecnico.dsi.vault.{Context, DSL}
import pt.tecnico.dsi.vault.secretEngines.identity.models._

/** @define name */
class AliasCRUD[F[_]: Sync: Client, T: Decoder](basePath: String, baseUri: Uri, baseName: String)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  val path: String = s"$basePath/$baseName-alias"
  val uri: Uri = baseUri / s"$baseName-alias"

  /** List $name aliases by their identifiers. */
  def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

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

  /**
    * Creates a new alias for a $name.
    * @param name name for the $name alias.
    * @param canonicalId $name ID of to which this alias belongs to.
    * @param mountAccessor mount accessor which this alias belongs to.
    */
  def create(name: String, canonicalId: String, mountAccessor: String): F[Unit] =
    execute(POST(Alias(name, canonicalId, mountAccessor, None), uri, token))

  /**
    * Updates an existing $name alias.
    * @param id ID of the $name alias.
    * @param name name of the $name alias.
    * @param canonicalId $name ID of to which this alias belongs to.
    * @param mountAccessor mount accessor which this alias belongs to.
    */
  def update(id: String, name: String, canonicalId: String, mountAccessor: String): F[Unit] =
    execute(POST(Alias(name, canonicalId, mountAccessor, Some(id)), uri / "id" / id, token))

  /**
    * Deletes a $name alias.
    * @param id identifier of the $name alias.
    */
  def delete(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
}

/**
  * @define name entity
  * @define namePlural entities
  */
class BaseEndpoints[F[_]: Sync: Client, T: Decoder](basePath: String, baseUri: Uri, name: String)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  val path: String = s"$basePath/$name"
  val uri: Uri = baseUri / s"$name"

  /** Lists $namePlural by their names. */
  def list(): F[List[String]] = executeWithContextKeys(LIST(uri / "name", token))
  /** Lists $namePlural by their identifiers. */
  def listById(): F[List[String]] = executeWithContextKeys(LIST(uri / "id", token))

  /** Gets the $name with the given `name`. */
  def get(name: String): F[Option[T]] = executeOptionWithContextData(GET(uri / "name" / name, token))
  /** Gets the $name with the given `name`, assuming it exists. */
  def apply(name: String): F[T] = executeWithContextData(GET(uri / "name" / name, token))
  /** Gets the $name with the given `id`. */
  def getById(id: String): F[Option[T]] = executeOptionWithContextData(GET(uri / "id" / id, token))

  /** Deletes the $name with `name` and all its associated aliases. */
  def delete(name: String): F[Unit] = execute(DELETE(uri / "name" / name, token))
  /** Deletes the $name with `id` and all its associated aliases. */
  def deleteById(id: String): F[Unit] = execute(DELETE(uri / "id" / id, token))
}

final class Identity[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

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
    def create(name: String, policies: List[String] = List.empty, disabled: Boolean = false, metadata: Map[String, String] = Map.empty): F[String] = {
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
          response.as[Context[String]].map(_.data)
        case NoContent(_) => apply(name).map(_.id)
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
  object entityAlias extends AliasCRUD[F, EntityAlias](path, uri, "entity")

  /**
    * @define name group
    * @define namePlural groups
    */
  object group extends BaseEndpoints[F, Group](path, uri, "group") {
    /**
      * Creates or updates a Group.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(group: GroupCreate): F[String] = {
      implicit val d: Decoder[String] = Decoder.decodeString.at("id")
      executeWithContextData[String](POST(group, uri, token))
    }
    /** Updates an existing group. */
    def update(id: String, group: GroupCreate): F[Unit] = execute(POST(group.asJson.mapObject(_.remove("id")), uri / "id" / id, token))
    /**
      * Create or update a group by a given name.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def createByName(entity: Entity): F[Unit] = execute(POST(entity.asJson.mapObject(_.remove("id")), uri / "name" / entity.name, token))
    /** Updates an existing group. */
    def updateByName(id: String, entity: Entity): F[Unit] = execute(POST(entity, uri / "id" / id, token))
  }

  /** @define name group */
  object groupAlias extends AliasCRUD[F, GroupAlias](path, uri, "group")
}