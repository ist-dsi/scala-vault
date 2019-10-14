package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import scala.concurrent.duration.Duration
import io.circe.derivation._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, JsonObject}
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseRole
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object Role {
  implicit val encoder: Encoder.AsObject[Role] = deriveEncoder[Role](renaming.snakeCase, None)
  implicit val decoder: Decoder[Role] = deriveDecoder[Role](renaming.snakeCase, false, None)

  def defaultCreationStatements(database: String = "admin", roles: List[MongoRole]) = JsonObject(
    "db" -> database.asJson,
    "roles" -> roles.asJson,
  )
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param creationStatements the database statements executed to create and configure a user.
  *                           See the plugin's API page for more information on support and formatting for this parameter.
  * @param revocationStatements the database statements to be executed to revoke a user.
  *                             See the plugin's API page for more information on support and formatting for this parameter.
  * @param defaultTtl the TTL for the leases associated with this role. Defaults to system/engine default TTL time.
  * @param maxTtl the maximum TTL for the leases associated with this role.
  *               Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL
  *               (or, if not set, the system max TTL), but it is not allowed to be longer.
  *               @see See also [[https://www.vaultproject.io/docs/concepts/tokens.html#the-general-case The TTL General Case]].
  */
case class Role private (dbName: String, creationStatements: List[String], revocationStatements: List[String],
                defaultTtl: Duration, maxTtl: Duration) extends BaseRole {
  def this(dbName: String, creationStatements: JsonObject, revocationStatements: List[String] = List.empty, defaultTtl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined) {
    this(dbName, List(creationStatements.asJson.noSpaces), revocationStatements, defaultTtl, maxTtl)
  }
}

object MongoRole {
  implicit val encoder: Encoder.AsObject[MongoRole] = deriveEncoder[MongoRole](renaming.snakeCase, None)
  implicit val decoder: Decoder[MongoRole] = deriveDecoder[MongoRole](renaming.snakeCase, false, None)
}
case class MongoRole(role: String, db: String)