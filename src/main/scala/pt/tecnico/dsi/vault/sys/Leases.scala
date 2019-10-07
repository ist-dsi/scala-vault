package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import io.circe.syntax._
import io.circe.generic.auto._
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.{Lease, LeaseRenew}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

class Leases[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /**
    * This endpoint returns a list of lease ids. This endpoint requires 'sudo' capability.
    *
    * @param prefix the prefix for which to list leases.
    */
  def list(prefix: String): F[List[String]] =
    executeWithContextKeys(LIST(uri.withPath(uri.path + "/lookup/" + prefix), token))

  def apply(name: String): F[Lease] = get(name).map(_.get)
  /**
    * @param id the id of the lease.
    * @return the metadata associated with the lease with `id`. If no lease with that `id` exists a None will be returned.
    */
  def get(id: String): F[Option[Lease]] =
    for {
      request <- PUT(Map("lease_id" -> id).asJson, uri / "lookup", token)
      response <- client.expectOption[Context[Lease]](request)
    } yield response.map(_.data)

  /**
    * Renews a lease, requesting to extend the lease.
    *
    * @param id Specifies the ID of the lease to extend.
    * @param increment Specifies the requested amount of time to extend the lease.
    * @return
    */
  def renew(id: String, increment: Duration = 0.second): F[LeaseRenew] = {
    case class Renew(lease_id: String, increment: Int)
    execute(PUT(Renew(id, increment.toSeconds.toInt).asJson, uri / "renew", token))
  }

  /**
    * Revokes a lease immediately.
    * @param id Specifies the ID of the lease to revoke.
    */
  def revoke(id: String): F[Unit] = execute(PUT(Map("lease_id" -> id).asJson, uri / "revoke", token))

  /**
    * This endpoint revokes all secrets or tokens generated under a given prefix immediately. Unlike [[revokePrefix]],
    * this ignores backend errors encountered during revocation. This is potentially very dangerous and should only
    * be used in specific emergency situations where errors in the backend or the connected backend service prevent
    * normal revocation.
    *
    * By ignoring these errors, Vault abdicates responsibility for ensuring that the issued credentials or secrets
    * are properly revoked and/or cleaned up. Access to this endpoint should be tightly controlled.
    *
    * This endpoint requires 'sudo' capability.
    *
    * @param prefix the prefix to revoke.
    */
  def revokeForce(prefix: String): F[Unit] = execute(PUT(uri / "revoke-force" / prefix, token))

  /**
    * This endpoint revokes all secrets (via a lease ID prefix) or tokens (via the tokens' path property) generated
    * under a given prefix immediately. This requires `sudo` capability and access to it should be tightly
    * controlled as it can be used to revoke very large numbers of secrets/tokens at once.
    *
    * @param prefix the prefix to revoke.
    */
  def revokePrefix(prefix: String): F[Unit] = execute(PUT(uri / "revoke-prefix" / prefix, token))
}
