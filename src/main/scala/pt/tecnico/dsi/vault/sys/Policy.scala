package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.{Policy => PolicyModel}

final class Policy[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return all configured policies. */
  def list(): F[List[String]] = {
    implicit val d = decoderDownField[List[String]]("policies")
    execute(GET(uri, token))
  }

  def apply(name: String): F[PolicyModel] = execute(GET(uri / name, token))
  /** @return the policy associated with `name`. */
  def get(name: String): F[Option[PolicyModel]] = executeOption(GET(uri / name, token))

  /** Adds a new or updates an existing policy. Once a policy is updated, it takes effect immediately to all associated users. */
  def create(name: String, policy: PolicyModel): F[Unit] = {
    // This endpoint is inconsistent with the read policy endpoint
    execute(PUT(Map("policy" -> policy.rules).asJson, uri / name, token))
  }
  /**
    * Alternative syntax to create a policy:
    * {{{ client.sys.policy += "a" -> Policy(...) }}}
    */
  def +=(tuple: (String, PolicyModel)): F[Unit] = create(tuple._1, tuple._2)
  /**
    * Allows creating multiple policies in one go:
    * {{{
    *   client.sys.policy ++= List(
    *     "a" -> Policy(...),
    *     "b" -> Policy(...),
    *   )
    * }}}
    */
  def ++=(list: List[(String, PolicyModel)]): F[Unit] = list.map(+=).sequence_

  /**
    * Deletes the policy with the given `name`. This will immediately affect all users associated with this policy.
    * @param name the name of the policy to delete.
    */
  def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
  /**
    * Alternative syntax to delete a policy:
    * {{{ client.sys.policy -= "name" }}}
    */
  def -=(name: String): F[Unit] = delete(name)
  /**
    * Allows deleting multiple policies in one go:
    * {{{
    *   client.sys.policy --= List("a", "b")
    * }}}
    */
  def --=(names: List[String]): F[Unit] = names.map(delete).sequence_
}
