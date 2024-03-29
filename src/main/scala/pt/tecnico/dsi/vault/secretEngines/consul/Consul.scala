package pt.tecnico.dsi.vault.secretEngines.consul

import cats.effect.Concurrent
import io.circe.Decoder
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{GET, POST}
import pt.tecnico.dsi.vault.{DSL, RolesCRUD}
import pt.tecnico.dsi.vault.secretEngines.consul.models.Role

final class Consul[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header.Raw) {
  private val dsl = new DSL[F] {}
  import dsl.*

  /**
    * Configures the access information for Consul. This access information is used so that Vault can communicate
    * with Consul and generate Consul tokens.
    * @param uri the address of the Consul instance. If uri does not have a host `localhost` will be used.
    *            If uri does not have a port 8500 will be used. If uri does not have a scheme `http` will be used.
    *            Thus the "default" uri will be http://localhost:8500.
    * @param token the Consul ACL token to use. This must be a management type token.
    */
  def configure(uri: Uri, token: String): F[Unit] = {
    import org.http4s.Uri.Scheme
    val data = Map(
      "address" -> s"${uri.host.map(_.renderString).getOrElse("localhost")}:${uri.port.getOrElse(8500)}",
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
