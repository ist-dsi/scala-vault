package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import cats.implicits.{catsStdInstancesForList, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.secretEngines.databases.models._

abstract class Databases[F[_]: Sync, Connection <: BaseConnection : Encoder : Decoder, Role <: BaseRole : Encoder : Decoder]
                        (uri: Uri)(implicit client: Client[F], token: Header) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  object connections {
    val uri: Uri = self.uri / "config"

    /** @return all available connections. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    def apply(name: String): F[Connection] = get(name).map(_.get)
    /** @return the connection associated with `name`. */
    def get(name: String): F[Option[Connection]] =
      for {
        request <- GET(uri / name, token)
        response <- client.expectOption[Connection](request)
      } yield response

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
    def ++=(list: List[(String, Connection)]): F[List[Unit]] = list.map(+=).sequence

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
    def --=(names: List[String]): F[List[Unit]] = names.map(delete).sequence

    /**
      * Closes a connection and it's underlying plugin and restarts it with the configuration stored in the barrier.
      * @param name the name of the connection to reset.
      */
    def reset(name: String): F[Unit] = execute(POST(self.uri / "reset" / name))
  }

  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param connection the name of the connection to rotate.
    */
  def rotateRootCredentials(connection: String): F[Unit] = execute(POST(uri / "rotate-root" / connection, token))

  object roles {
    val uri: Uri = self.uri / "roles"

    /** List the available roles. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    def apply(name: String): F[Role] = get(name).map(_.get)
    /** @return the role associated with `name`. */
    def get(name: String): F[Option[Role]] =
      for {
        request <- GET(uri / name, token)
        response <- client.expectOption[Context[Role]](request)
      } yield response.map(_.data)

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
    def ++=(list: List[(String, Role)]): F[List[Unit]] = list.map(+=).sequence

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
    def --=(names: List[String]): F[List[Unit]] = names.map(delete).sequence
  }

  /**
    * Rotates the root superuser credentials stored for the database connection.
    * This user must have permissions to update its own password.
    *
    * @param role the name of the role to create credentials against.
    */
  def generateCredentials(role: String): F[Credential] = executeWithContextData[Credential](GET(uri / "creds" / role, token))

  // TODO: do we also need to abstract the StaticRole to a type parameter?
  object staticRoles {
    val uri: Uri = self.uri / "static-roles"

    /** List the available roles. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    def apply(name: String): F[StaticRole] = get(name).map(_.get)
    /** @return the role associated with `name`. */
    def get(name: String): F[Option[StaticRole]] =
      for {
        request <- GET(uri / name, token)
        response <- client.expectOption[Context[StaticRole]](request)
      } yield response.map(_.data)

    /**
      * Creates or updates a role definition.
      *
      * Static Roles are a 1-to-1 mapping of a Vault Role to a user in a database which are automatically rotated based
      * on the configured rotationPeriod. Not all databases support Static Roles, please see the database-specific documentation.
      *
      * @note This endpoint distinguishes between create and update ACL capabilities. */
    def create(name: String, role: StaticRole): F[Unit] = execute(POST(role, uri / name, token))
    /**
      * Alternative syntax to create a role:
      * * {{{ client.secretEngines.database("path").staticRoles += "a" -> Role(...) }}}
      */
    def +=(tuple: (String, StaticRole)): F[Unit] = create(tuple._1, tuple._2)
    /**
      * Allows creating multiple roles in one go:
      * {{{
      *   client.secretEngines.database("path").staticRoles ++= List(
      *     "a" -> Role(...),
      *     "b" -> Role(...),
      *   )
      * }}}
      */
    def ++=(list: List[(String, StaticRole)]): F[List[Unit]] = list.map(+=).sequence

    /** Deletes the static role definition and revokes the database user. */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
    /**
      * Alternative syntax to delete a role:
      * * {{{ client.secretEngines.database("path").staticRoles -= "a" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple roles in one go:
      * {{{
      *   client.secretEngines.database("path").staticRoles --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[List[Unit]] = names.map(delete).sequence
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
