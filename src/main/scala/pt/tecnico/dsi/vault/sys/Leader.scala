package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{GET, PUT}
import pt.tecnico.dsi.vault.*
import pt.tecnico.dsi.vault.sys.models.LeaderStatus

final class Leader[F[_]: Concurrent: Client](uri: Uri)(implicit token: Header.Raw) {
  private val dsl = new DSL[F] {}
  import dsl.*

  /** @return the high availability status and current leader instance of Vault. */
  val status: F[LeaderStatus] = execute(GET(uri / "leader"))

  /**
    * Forces the node to give up active status. If the node does not have active status, this does nothing.
    * Note that the node will sleep for ten seconds before attempting to grab the active lock again, but if no
    * standby nodes grab the active lock in the interim, the same node may become the active node again.
    * Requires a token with `root` policy or `sudo` capability on the path.
    */
  val stepDown: F[Unit] = execute(PUT(uri / "step-down", token))
}
