package pt.tecnico.dsi.vault.secretEngines.consul

import cats.effect.Sync
import io.circe.Decoder
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{GET, POST}
import pt.tecnico.dsi.vault.{DSL, RolesCRUD}
import pt.tecnico.dsi.vault.secretEngines.consul.models.Role

final class Consul[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) {
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
      "address" -> s"${uri.host.getOrElse("localhost")}:${uri.port.getOrElse(8500)}",
      "scheme" -> uri.scheme.getOrElse(Scheme.http).value,
      "token" -> token
    )
    execute(POST(data, this.uri / "config" / "access", this.token))
  }

  /**
    * Generates a dynamic Consul token based on the given role definition.
    * @param role the name of an existing role against which to create this Consul credential.
    */
  def generateCredential(role: String): F[String] = {
    implicit val d = Decoder[String].at("token")
    executeWithContextData(GET(uri / "creds" / role, token))
  }

  object roles extends RolesCRUD[F, Role](path, uri)
}
