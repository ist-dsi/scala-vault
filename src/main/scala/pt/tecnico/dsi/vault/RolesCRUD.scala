package pt.tecnico.dsi.vault

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import io.circe.{Decoder, Encoder}
import org.http4s.{Header, Uri}
import org.http4s.client.Client

class RolesCRUD[F[_]: Sync: Client, Role: Encoder: Decoder](basePath: String, baseUri: Uri)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  val path: String = s"$basePath/roles"
  val uri: Uri = baseUri / "roles"

  /** List the available roles by name. */
  def list(): F[List[String]] = executeWithContextKeys(LIST(uri, token))

  /**
    * Gets the role with the given name.
    *
    * @param name the name of the role.
    * @return if a role named `name` exists a `Some` will be returned. `None` otherwise.
    */
  def get(name: String): F[Option[Role]] = executeOptionWithContextData(GET(uri / name, token))
  /**
    * Gets the role with the given name.
    *
    * @param name the name of the role.
    */
  def apply(name: String): F[Role] = executeWithContextData(GET(uri / name, token))

  /**
    * Creates or updates a role definition.
    *
    * @note This endpoint distinguishes between create and update ACL capabilities. */
  def create(name: String, role: Role): F[Unit] = execute(POST(role, uri / name, token))
  /** Alternative syntax to create a role. */
  def +=(tuple: (String, Role)): F[Unit] = create(tuple._1, tuple._2)
  /** Allows creating multiple roles in one go. */
  def ++=(list: List[(String, Role)]): F[Unit] = list.map(+=).sequence_

  /**
    * Deletes the role with the given name.
    * @param name the role to delete.
    */
  def delete(name: String): F[Unit] = execute(DELETE(uri / name, token))
  /** Alternative syntax to delete a role. */
  def -=(name: String): F[Unit] = delete(name)
  /** Allows deleting multiple roles in one go. */
  def --=(names: List[String]): F[Unit] = names.map(delete).sequence_
}
