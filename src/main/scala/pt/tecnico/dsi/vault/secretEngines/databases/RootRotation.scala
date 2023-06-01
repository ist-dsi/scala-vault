package pt.tecnico.dsi.vault.secretEngines.databases

import org.http4s.Method.POST

trait RootRotation[F[_]] { self: Databases[F, ?, ?] =>
  import dsl.*
  
  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param connection the name of the connection to rotate.
    */
  def rotateRootCredentials(connection: String): F[Unit] = execute(POST(uri / "rotate-root" / connection, token))
}