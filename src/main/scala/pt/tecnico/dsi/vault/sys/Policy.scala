package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.foldable._
import io.circe.generic.auto._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.{Policy => PolicyModel}

class Policy[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return all configured policies. */
  def list(): F[List[String]] = {
    case class Policies(policies: List[String])
    execute[Policies](GET(uri, token)).map(_.policies)
  }

  def apply(name: String): F[PolicyModel] = get(name).map(_.get)
  /** @return the policy associated with `name`. */
  def get(name: String): F[Option[PolicyModel]] =
    for {
      request <- GET(uri / name, token)
      response <- client.expectOption[PolicyModel](request)
    } yield response

  /** Adds a new or updates an existing policy. Once a policy is updated, it takes effect immediately to all associated users. */
  def create(policy: PolicyModel): F[Unit] = execute(PUT(policy, uri / policy.name, token))
  /**
    * Alternative syntax to create a policy:
    * {{{ client.sys.policy += Policy(...) }}}
    */
  def +=(policy: PolicyModel): F[Unit] = create(policy)
  /**
    * Allows creating multiple policies in one go:
    * {{{
    *   client.sys.policy ++= List(
    *     Policy(...),
    *     Policy(...),
    *   )
    * }}}
    */
  def ++=(list: List[PolicyModel]): F[Unit] = list.map(create).sequence_

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
