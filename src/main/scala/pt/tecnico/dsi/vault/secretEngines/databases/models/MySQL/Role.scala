package pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL

import scala.concurrent.duration.Duration
import cats.data.NonEmptyList
import io.circe.derivation._
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseRole
import pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL.Role._
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

object Role {
  implicit val encoder = deriveEncoder[Role](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[Role](renaming.snakeCase, false, None)

  def defaultCreationStatements(permissions: NonEmptyList[String] = NonEmptyList.one("ALL"), privLevel: String = "*.*", host: String = "%") = List(
    s"CREATE USER '{{name}}'@'$host' IDENTIFIED BY '{{password}}';",
    s"GRANT ${permissions.toList.mkString(",")} ON $privLevel TO '{{name}}'@'$host'",
  )
  def defaultRevocationStatements(host: String = "%") = List(
    s"REVOKE ALL PRIVILEGES, GRANT OPTION FROM '{{name}}'@'$host';",
    s"DROP USER '{{name}}'@'$host';",
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
case class Role(dbName: String, creationStatements: List[String], revocationStatements: List[String] = defaultRevocationStatements(),
                defaultTtl: Duration = Duration.Undefined, maxTtl: Duration = Duration.Undefined) extends BaseRole