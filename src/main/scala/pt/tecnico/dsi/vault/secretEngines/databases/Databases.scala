package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import io.circe.Codec
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.secretEngines.databases.models._

abstract class Databases[F[_], Connection <: BaseConnection : Codec, Role <: BaseRole : Codec]
                        (val uri: Uri)(implicit protected val client: Client[F], protected val token: Header, protected val F: Sync[F]) { self =>
  protected val dsl: DSL[F] = new DSL[F] {}
  import dsl._

  object connections {
    val uri: Uri = self.uri / "config"

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
      * Alternative syntax to create a connection:
      * {{{ client.secretEngines.database("path").connections += Connection(...) }}}
      */
    def +=(tuple: (String, Connection)): F[Unit] = create(tuple._1, tuple._2)
    /**
      * Allows creating multiple connections in one go:
      * {{{
      *   client.secretEngines.database("path").connections ++= List(
      *     Connection(...),
      *     Connection(...),
      *   )
      * }}}
      */
    def ++=(list: List[(String, Connection)]): F[Unit] = list.map(+=).sequence_

    /**
      * Deletes the connection with the given `name`.
      * @param name the name of the connection to delete.
      */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
    /**
      * Alternative syntax to delete a connection:
      * {{{ client.secretEngines.database("path").connections -= "name" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple connection in one go:
      * {{{
      *   client.secretEngines.database("path").connections --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[Unit] = names.map(delete).sequence_

    /**
      * Closes a connection and it's underlying plugin and restarts it with the configuration stored in the barrier.
      * @param name the name of the connection to reset.
      */
    def reset(name: String): F[Unit] = execute(POST(self.uri / "reset" / name))
  }

  object roles {
    val uri: Uri = self.uri / "roles"

    /** List the available roles. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    def apply(name: String): F[Role] = executeWithContextData(GET(uri / name, token))
    /** @return the role associated with `name`. */
    def get(name: String): F[Option[Role]] = executeOptionWithContextData(GET(uri / name, token))

    /**
      * Creates or updates a role definition.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(name: String, role: Role): F[Unit] = execute(POST(role, uri / name, token))
    /**
      * Alternative syntax to create a role:
      * * {{{ client.secretEngines.database("path").roles += "a" -> Role(...) }}}
      */
    def +=(tuple: (String, Role)): F[Unit] = create(tuple._1, tuple._2)
    /**
      * Allows creating multiple roles in one go:
      * {{{
      *   client.secretEngines.database("path").roles ++= List(
      *     "a" -> Role(...),
      *     "b" -> Role(...),
      *   )
      * }}}
      */
    def ++=(list: List[(String, Role)]): F[Unit] = list.map(+=).sequence_

    /** Delete the role with the given `name`. */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
    /**
      * Alternative syntax to delete a role:
      * * {{{ client.secretEngines.database("path").roles -= "a" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple roles in one go:
      * {{{
      *   client.secretEngines.database("path").roles --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[Unit] = names.map(delete).sequence_
  }

  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param role the name of the role to create credentials against.
    */
  def generateCredentials(role: String): F[Credential] = executeWithContextData[Credential](GET(uri / "creds" / role, token))
}
