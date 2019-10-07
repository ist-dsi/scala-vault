package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.HealthStatus

class Health[F[_]: Sync](uri: Uri)(implicit client: Client[F]) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the health status of Vault. This matches the semantics of a Consul HTTP health check and provides a
    *         simple way to monitor the health of a Vault instance.
    */
  def status(): F[HealthStatus] = execute(GET(uri))
}
