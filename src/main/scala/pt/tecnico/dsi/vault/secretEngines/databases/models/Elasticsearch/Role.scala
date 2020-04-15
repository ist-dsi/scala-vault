package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import scala.concurrent.duration.Duration
import io.circe.{Codec, JsonObject}
import io.circe.derivation.{deriveCodec, renaming}
import io.circe.syntax._
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseRole

object Role {
  implicit val codec: Codec.AsObject[Role] = deriveCodec(renaming.snakeCase, false, None)
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param creationStatements the database statements executed to create and configure a user.
  *                           See the plugin's API page for more information on support and formatting for this parameter.
  * @param defaultTtl the TTL for the leases associated with this role. Defaults to system/engine default TTL time.
  * @param maxTtl the maximum TTL for the leases associated with this role.
  *               Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL
  *               (or, if not set, the system max TTL), but it is not allowed to be longer.
  *               @see See also [[https://www.vaultproject.io/docs/concepts/tokens.html#the-general-case The TTL General Case]].
  */
case class Role private (dbName: String, creationStatements: List[String], defaultTtl: Duration, maxTtl: Duration) extends BaseRole {
  def this(dbName: String, creationStatements: JsonObject, defaultTtl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined) {
    this(dbName, List(creationStatements.asJson.noSpaces), defaultTtl, maxTtl)
  }
}