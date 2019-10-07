package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.LeaderStatus

class Leader[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the high availability status and current leader instance of Vault. */
  def status(): F[LeaderStatus] = execute(GET(uri / "leader"))

  /**
    * Forces the node to give up active status. If the node does not have active status, this does nothing.
    * Note that the node will sleep for ten seconds before attempting to grab the active lock again, but if no
    * standby nodes grab the active lock in the interim, the same node may become the active node again.
    * Requires a token with `root` policy or `sudo` capability on the path.
    */
  def stepDown(): F[Unit] = execute(PUT(uri / "step-down", token))
}
