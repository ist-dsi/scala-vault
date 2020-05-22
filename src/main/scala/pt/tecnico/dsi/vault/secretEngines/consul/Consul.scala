package pt.tecnico.dsi.vault.secretEngines.consul

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.secretEngines.consul.models.Role

final class Consul[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  /**
    * Configures the access information for Consul. This access information is used so that Vault can communicate
    * with Consul and generate Consul tokens.
    * @param uri the address of the Consul instance.
    * @param token the Consul ACL token to use. This must be a management type token.
    */
  def configure(uri: Uri, token: String): F[Unit] = {
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
    implicit val d = decoderDownField[String]("token")
    executeWithContextData(GET(uri / "creds" / role, token))
  }

  object roles {
    val path: String = s"${self.path}/roles"
    val uri: Uri = self.uri / "roles"

    /** Lists all existing roles in the secrets engine. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

    /**
      * Gets the information associated with a Consul role with the given name.
      * @param name the name of the role.
      */
    def get(name: String): F[Option[Role]] = executeOptionWithContextData(GET(uri / name, token))
    def apply(name: String): F[Role] = executeWithContextData(GET(uri / name, token))

    def create(name: String, role: Role): F[Unit] = execute(POST(role.asJson, uri / name, token))
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
    def ++=(list: List[(String, Role)]): F[Unit] = list.map(+=).sequence_

    /**
      * Deletes a Consul role with the given name.
      * @param name the role to delete
      */
    def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
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
    def --=(names: List[String]): F[Unit] = names.map(delete).sequence_
  }
}