package pt.tecnico.dsi.vault.secretEngines.databases

trait RootRotation[F[_]] { self: Databases[F, _, _] =>
  import dsl._

  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param connection the name of the connection to rotate.
    */
  def rotateRootCredentials(connection: String): F[Unit] = execute(POST(uri / "rotate-root" / connection, token))
}