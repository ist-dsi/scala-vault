package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import io.circe.Decoder
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.{Policy => PolicyModel}

final class Policy[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return all configured policies. */
  def list(): F[List[String]] = {
    implicit val d = Decoder[List[String]].at("policies")
    execute(GET(uri, token))
  }

  def apply(name: String): F[PolicyModel] = execute(GET(uri / name, token))
  /** @return the policy associated with `name`. */
  def get(name: String): F[Option[PolicyModel]] = executeOption(GET(uri / name, token))

  /** Adds a new or updates an existing policy. Once a policy is updated, it takes effect immediately to all associated users. */
  def create(name: String, policy: PolicyModel): F[Unit] = {
    // This endpoint is inconsistent with the read policy endpoint
    execute(PUT(Map("policy" -> policy.rules), uri / name, token))
  }

  /**
    * Deletes the policy with the given `name`. This will immediately affect all users associated with this policy.
    * @param name the name of the policy to delete.
    */
  def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
}
