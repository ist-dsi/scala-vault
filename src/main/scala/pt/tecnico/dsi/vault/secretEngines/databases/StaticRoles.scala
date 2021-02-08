package pt.tecnico.dsi.vault.secretEngines.databases

import org.http4s.Method.{DELETE, GET, POST}
import org.http4s.Uri
import pt.tecnico.dsi.vault.secretEngines.databases.models.{StaticCredential, StaticRole}

trait StaticRoles[F[_]] { self: Databases[F, _, _] =>
  import dsl._

  object staticRoles {
    val uri: Uri = self.uri / "static-roles"

    /** List the available roles. */
    val list: F[List[String]] = executeWithContextKeys(LIST(uri, token))

    /** @return the role associated with `name`. */
    def get(name: String): F[Option[StaticRole]] = executeOptionWithContextData(GET(uri / name, token))
    def apply(name: String): F[StaticRole] = executeWithContextData(GET(uri / name, token))

    /**
      * Creates or updates a role definition.
      *
      * Static Roles are a 1-to-1 mapping of a Vault Role to a user in a database which are automatically rotated based
      * on the configured rotationPeriod. Not all databases support Static Roles, please see the database-specific documentation.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(name: String, role: StaticRole): F[Unit] = execute(POST(role, uri / name, token))

    /** Deletes the static role definition and revokes the database user. */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
  }

  /**
    * @return the current credentials based on the named static role.
    *
    * @param staticRole the name of the static role to get credentials for.
    */
  def getStaticCredentials(staticRole: String): F[StaticCredential] = executeWithContextData[StaticCredential](GET(uri / "static-creds" / staticRole, token))

  /**
    * Rotate the Static Role credentials stored for a given role name. While Static Roles are rotated automatically by
    * Vault at configured rotation periods, users can use this endpoint to manually trigger a rotation to change the
    * stored password and reset the TTL of the Static Role's password.
    *
    * @param staticRole the name of the connection to rotate.
    */
  def rotateStaticRootCredentials(staticRole: String): F[Unit] = execute(POST(uri / "rotate-role" / staticRole, token))
}
