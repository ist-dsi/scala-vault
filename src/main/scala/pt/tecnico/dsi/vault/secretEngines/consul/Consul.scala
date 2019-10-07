package pt.tecnico.dsi.vault.secretEngines.consul

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.secretEngines.consul.models.Role

class Consul[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /**
    * Configures the access information for Consul. This access information is used so that Vault can communicate
    * with Consul and generate Consul tokens.
    * @param uri the address of the Consul instance.
    * @param token the Consul ACL token to use. This must be a management type token.
    */
  def configureAccess(uri: Uri, token: String): F[Unit] = {
    import org.http4s.Uri.Scheme
    val data = Map(
      "address" -> s"${uri.host}:${uri.port}",
      "scheme" -> uri.scheme.getOrElse(Scheme.http).value,
      "token" -> token
    )
    execute(POST(data.asJson, uri / "config" / "access", this.token))
  }

  /**
    * Generates a dynamic Consul token based on the given role definition.
    * @param role the name of an existing role against which to create this Consul credential.
    */
  def generateCredential(role: String): F[String] = {
    case class CredentialResponse(token: String)
    executeWithContextData[CredentialResponse](GET(uri / "creds" / role, token)).map(_.token)
  }

  object roles {
    private val rolesUri = uri / "roles"

    /** Lists all existing roles in the secrets engine. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(rolesUri, token))

    /**
      * Gets the information associated with a Consul role with the given name.
      * @param name the name of the role.
      */
    def get(name: String): F[Option[Role]] =
      for {
        request <- GET(rolesUri / name, token)
        response <- client.expectOption[Context[Role]](request)
      } yield response.map(_.data)
    def apply(name: String): F[Role] = get(name).map(_.get)

    def create(name: String, role: Role): F[Unit] = {
      execute(POST(role.asJson, rolesUri / name, token))
    }
    /**
      * Alternative syntax to create a role:
      * * {{{ client.secretEngines.consul.roles += "a" -> Role(...) }}}
      */
    def +=(tuple: (String, Role)): F[Unit] = create(tuple._1, tuple._2)
    /**
      * Allows creating multiple roles in one go:
      * {{{
      *   client.secretEngines.consul.roles ++= List(
      *     "a" -> Role(...),
      *     "b" -> Role(...),
      *   )
      * }}}
      */
    def ++=(list: List[(String, Role)]): F[List[Unit]] = {
      import cats.instances.list._
      list.map(+=).sequence
    }

    /**
      * Deletes a Consul role with the given name.
      * @param name the role to delete
      */
    def delete(name: String): F[Unit] = execute(DELETE(rolesUri / name, token))
    /**
      * Alternative syntax to delete a role:
      * {{{ client.secretEngines.consul.roles -= "role-name" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple roles in one go:
      * {{{
      *   client.secretEngines.consul.roles --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[List[Unit]] = {
      import cats.instances.list._
      names.map(delete).sequence
    }
  }
}