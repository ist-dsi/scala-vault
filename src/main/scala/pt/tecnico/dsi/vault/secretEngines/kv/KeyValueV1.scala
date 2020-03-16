package pt.tecnico.dsi.vault.secretEngines.kv

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.{DSL, RichUri}

class KeyValueV1[F[_]: Sync](val path: String, val uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  //TODO: the paths are being url encoded, we do not want that directly

  /**
    * Returns a list of key names at the specified location. Folders are suffixed with /.
    * The input must be a folder; list on a file will not return a value.
    * Note that no policy-based filtering is performed on keys; do not encode sensitive information in key names.
    * The values themselves are not accessible via this command.
    * @param path the path to list the secrets keys.
    */
  def list(path: String): F[List[String]] = executeWithContextKeys(LIST(uri append path, token))

  /**
    * Retrieves the secret at the specified `path`.
    * @param path the path from which to retrieve the secret.
    */
  def read(path: String): F[Option[Map[String, String]]] = executeOptionWithContextData(GET(uri append path, token))
  def apply(path: String): F[Map[String, String]] = executeWithContextData(GET(uri append path, token))

  /**
    * Stores a secret at the specified location.
    * If the value does not yet exist, the calling token must have an ACL policy granting the `create` capability.
    * If the value already exists, the calling token must have an ACL policy granting the `update` capability.
    * @param path the path at which to create the secret.
    * @param secret the secret.
    */
  def create(path: String, secret: Map[String, String]): F[Unit] = execute(PUT(secret, uri append path, token))
  /**
    * Stores a secret at the specified location.
    * If the value does not yet exist, the calling token must have an ACL policy granting the `create` capability.
    * If the value already exists, the calling token must have an ACL policy granting the `update` capability.
    * @param path the path at which to create the secret.
    * @param secret the secret.
    */
  def create(path: String, secret: (String, String)*): F[Unit] = create(path, secret.toMap)
  /**
    * Stores a secret of type `A` at the specified location. Given that `A` can be encoded as a Json Object.
    * If the value does not yet exist, the calling token must have an ACL policy granting the `create` capability.
    * If the value already exists, the calling token must have an ACL policy granting the `update` capability.
    * @param path the path at which to create the secret.
    * @param secret the secret.
    * @tparam A the type of the secret to be created
    */
  def create[A: Encoder.AsObject](path: String, secret: A): F[Unit] = execute(PUT(secret.asJson, uri append path, token))
  /**
    * Alternative syntax to create a secret:
    * * {{{ client.secretEngines.kv += "a" -> Map(...) }}}
    */
  def +=(path: String, secret: Map[String, String]): F[Unit] = create(path, secret)
  /**
    * Alternative syntax to create a secret:
    * * {{{ client.secretEngines.kv += "a" -> MyClass(...) }}}
    */
  def +=[A: Encoder.AsObject](path: String, secret: A): F[Unit] = create(path, secret)

  /**
    * Deletes the secret at the specified `path`.
    * @param path the path where the secret to be deleted resides.
    */
  def delete(path: String): F[Unit] = execute(DELETE(uri append path, token))
  /**
    * Alternative syntax to delete a secret:
    * {{{ client.secretEngines.kv -= "path" }}}
    */
  def -=(path: String): F[Unit] = delete(path)
  /**
    * Allows deleting multiple secrets in one go:
    * {{{
    *   client.secretEngines.kv --= List("path-a", "path-b")
    * }}}
    */
  def --=(path: List[String]): F[Unit] = path.map(delete).sequence_
}