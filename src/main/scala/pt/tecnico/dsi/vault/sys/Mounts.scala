package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.SecretEngine
import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions

class Mounts[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists all the mounted secrets engines. */
  def list(): F[Map[String, SecretEngine]] = executeWithContextData(GET(uri, token))

  // It would be nice to return the mounted engine directly. Eg:
  // Instead of the users doing:
  //    _ <- client.sys.mounts.enable(path, pkiSecretEngine)
  //    pki = client.secretEngines.pki(path)
  // The would do
  //    pki <- client.sys.mounts.enable(path, pkiSecretEngine)
  // However to make this work we would need path dependent types. And IDEs might not handle them nicely.
  // We would also need a specific type for each SecretEngine. That class could already set the `type`.

  /**
    * Enables a new secrets engine at the given path.
    * @param path Specifies the path where the secrets engine will be mounted.
    * @param engine the secret engine to enable.
    */
  def enable(path: String, engine: SecretEngine): F[Unit] = {
    executeHandlingErrors(POST(engine, uri / path, token)) {
      case errors if errors.exists(_.contains("path is already in use at")) =>
        apply(path).flatMap {
          case options if options == engine.config => implicitly[Sync[F]].unit
          case _ => tune(path, engine.config)
        }
    }
  }
  /**
    * Alternative syntax to enable a secret engine:
    * {{{ client.sys.mounts += "my-path" -> SecretEngine(...) }}}
    */
  def +=(tuple: (String, SecretEngine)): F[Unit] = enable(tuple._1, tuple._2)
  /**
    * Allows enabling multiple secrets engines in one go:
    * {{{
    *   client.sys.mounts ++= List(
    *     "my-path" -> SecretEngine(...),
    *     "your-path" -> SecretEngine(...),
    *   )
    * }}}
    */
  def ++=(list: List[(String, SecretEngine)]): F[List[Unit]] = list.map(+=).sequence

  /**
    * Disables the mount point at `path`.
    * @param path Specifies the path of the secrets engine that will be disabled.
    */
  def disable(path: String): F[Unit] = execute(DELETE(uri / path, token))
  /**
    * Alternative syntax to disable a secret engine:
    * {{{ client.sys.mounts -= "my-path" }}}
    */
  def -=(path: String): F[Unit] = disable(path)
  /**
    * Allows disabling multiple secrets engines in one go:
    * {{{
    *   client.sys.mounts --= List("my-path", "your-path")
    * }}}
    */
  def --=(paths: List[String]): F[List[Unit]] = paths.map(disable).sequence

  /**
    * Reads the given mount's configuration. Unlike the mounts endpoint, this will return the current time in seconds
    * for each TTL, which may be the system default or a mount-specific value.
    * @param path Specifies the path for the secrets engine.
    * @return
    */
  def tuneOptions(path: String): F[Option[TuneOptions]] =
    for {
      request <- GET(uri / path / "tune", token)
      response <- client.expectOption[Context[TuneOptions]](request)
    } yield response.map(_.data)
  def apply(path: String): F[TuneOptions] = tuneOptions(path).map(_.get)

  /**
    * Tunes configuration parameters for a given mount point.
    * @param path Specifies the path for the secrets engine.
    * @param options the secret engine tune options to tune.
    */
  def tune(path: String, options: TuneOptions): F[Unit] = execute(POST(options, uri / path / "tune", token))

  /**
    * Moves an already-mounted backend to a new mount point.
    * @param from Specifies the previous mount point.
    * @param to Specifies the new destination mount point.
    */
  def remount(from: String, to: String): F[Unit] = {
    val body = Map("from" -> from, "to" -> to)
    // TODO: find a better way to compute the uri
    execute(POST(body.asJson, uri.withPath("/v1/sys/remount"), token))
  }
}
