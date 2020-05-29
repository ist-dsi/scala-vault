package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import io.circe.Codec
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.{DSL, RolesCRUD}
import pt.tecnico.dsi.vault.secretEngines.databases.models._

abstract class Databases[F[_]: Client, Connection <: BaseConnection : Codec, Role <: BaseRole : Codec]
                        (val path: String, val uri: Uri)(implicit protected val token: Header, protected val F: Sync[F]) { self =>
  protected val dsl: DSL[F] = new DSL[F] {}
  import dsl._

  object connections {
    val path: String = "config"
    val uri: Uri = self.uri / path

    /** @return all available connections. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    /** @return the connection associated with `name`. */
    def get(name: String): F[Option[Connection]] = executeOptionWithContextData(GET(uri / name, token))
    def apply(name: String): F[Connection] = executeWithContextData(GET(uri / name, token))

    /**
      * Configures the connection string used to communicate with the desired database.
      * In addition to the parameters listed here, each Database plugin has additional, database plugin specific, parameters for this endpoint.
      * Please read the HTTP API for the plugin you'd wish to configure to see the full list of additional parameters.
      * @note This endpoint distinguishes between create and update ACL capabilities.
      **/
    def create(name: String, connection: Connection): F[Unit] = execute(POST(connection, uri / name, token))

    /**
      * Deletes the connection with the given `name`.
      * @param name the name of the connection to delete.
      */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))

    /**
      * Closes a connection and it's underlying plugin and restarts it with the configuration stored in the barrier.
      * @param name the name of the connection to reset.
      */
    def reset(name: String): F[Unit] = execute(POST(self.uri / "reset" / name))
  }

  object roles extends RolesCRUD[F, Role](path, uri)

  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param role the name of the role to create credentials against.
    */
  def generateCredentials(role: String): F[Credential] = executeWithContextData[Credential](GET(uri / "creds" / role, token))
}
