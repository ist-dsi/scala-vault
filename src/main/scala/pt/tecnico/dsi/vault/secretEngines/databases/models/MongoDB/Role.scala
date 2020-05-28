package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import scala.concurrent.duration.Duration
import io.circe._
import io.circe.derivation.{deriveCodec, renaming}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.Decoder.Result
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseRole

object MongoRole {
  implicit val codec: Codec.AsObject[MongoRole] = deriveCodec(renaming.snakeCase, true, None)
}
case class MongoRole(role: String, db: String)

object Role {
  val encoder: Encoder[Role] = Encoder.forProduct5("db_name", "creation_statements", "revocation_statements", "default_ttl", "max_ttl")(r =>
    (r.dbName, r.creationStatements, r.revocationStatements, r.defaultTtl, r.maxTtl)
  )
  val decoder: Decoder[Role] = new Decoder[Role] {
    private def decodeTo[A: Decoder](cursor: HCursor, at: String) =
      decode[A](at).left.map(e => DecodingFailure(e.getMessage, cursor.downField(at).history))

    override def apply(c: HCursor): Result[Role] =
      for {
        dbName <- c.get[String]("db_name")
        creationStatementsJson <- decodeTo[JsonObject](c, "creation_statements")
        revocationStatementsJson <- decodeTo[JsonObject](c, "revocation_statements")
        defaultTtl <- c.get[Duration]("default_ttl")
        maxTtl <- c.get[Duration]("max_ttl")
      } yield Role(dbName, creationStatementsJson, revocationStatementsJson, defaultTtl, maxTtl)
  }
  implicit val codec: Codec[Role] = Codec.from(decoder, encoder)

  def defaultCreationStatements(database: String = "admin", roles: List[MongoRole]) = JsonObject(
    "db" -> database.asJson,
    "roles" -> roles.asJson,
  )
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param creationStatementsJson Specifies the database statements executed to create and configure a user. The object can optionally
  *                               contain a "db" string for session connection, and must contain a "roles" array. This array contains objects
  *                               that holds a "role", and an optional "db" value, and is similar to the BSON document that is accepted by
  *                               MongoDB's roles field. Vault will transform this array into such format.
  *                               For more information regarding the roles field, refer to
  *                               <a href="https://docs.mongodb.com/manual/reference/method/db.createUser/">MongoDB's documentation</a>.
  * @param revocationStatementsJson Specifies the database statements to be executed to revoke a user.
  *                                 The object can optionally contain a "db" string. If no "db" value is provided, it defaults to the "admin" database.
  * @param defaultTtl the TTL for the leases associated with this role. Defaults to system/engine default TTL time.
  * @param maxTtl the maximum TTL for the leases associated with this role.
  *               Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL
  *               (or, if not set, the system max TTL), but it is not allowed to be longer.
  *               @see See also [[https://www.vaultproject.io/docs/concepts/tokens.html#the-general-case The TTL General Case]].
  */
case class Role(dbName: String, creationStatementsJson: JsonObject, revocationStatementsJson: JsonObject, defaultTtl: Duration, maxTtl: Duration)
  extends BaseRole {
  override val creationStatements: List[String] = List(creationStatementsJson.asJson.noSpaces)
  val revocationStatements: List[String] = List(revocationStatementsJson.asJson.noSpaces)
}