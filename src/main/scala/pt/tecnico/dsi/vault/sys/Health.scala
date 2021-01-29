package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.Method.GET
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.HealthStatus

final class Health[F[_]: Concurrent: Client](val path: String, val uri: Uri) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the health status of Vault. This matches the semantics of a Consul HTTP health check and provides a
    *         simple way to monitor the health of a Vault instance.
    */
  def status(): F[HealthStatus] = execute(GET(uri))
}
