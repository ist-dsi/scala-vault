package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.AuthMethod
import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions

/**
  * @define sudoRequired <b>sudo required</b> – This endpoint requires sudo capability in addition to any path-specific capabilities.
  */
final class Auth[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists all enabled auth methods. */
  def list(): F[Map[String, AuthMethod]] = executeWithContextData(GET(uri, token))

  /**
    * Enables a new auth method. After enabling, the auth method can be accessed and configured via the auth path specified as part of the URL.
    * This auth path will be nested under the auth prefix. For example, enable the "foo" auth method will make it accessible at /auth/foo.
    *
    * $sudoRequired
    *
    * @param path Specifies the path in which to enable the auth method.
    * @param method the authentication method to enable.
    */
  def enable(path: String, method: AuthMethod): F[Unit] = {
    executeHandlingErrors(POST(method, uri / path, token)) {
      case errors if errors.exists(_.contains("path is already in use at")) =>
        apply(path).flatMap {
          case config if config == method.config => implicitly[Sync[F]].unit
          case _ => tune(path, method.config)
        }
    }
  }
  /**
    * Alternative syntax to enable an authentication method:
    * {{{ client.sys.auth += "my-path" -> AuthMethod(...) }}}
    */
  def +=(tuple: (String, AuthMethod)): F[Unit] = enable(tuple._1, tuple._2)
  /**
    * Allows enabling multiple authentication methods in one go:
    * {{{
    *   client.sys.auth ++= List(
    *     "my-path" -> AuthMethod(...),
    *     "your-path" -> AuthMethod(...),
    *   )
    * }}}
    */
  def ++=(list: List[(String, AuthMethod)]): F[List[Unit]] = list.map(+=).sequence

  /**
    * Disables the auth method at the given auth path.
    *
    * $sudoRequired
    *
    * @param path Specifies the path to disable.
    */
  def disable(path: String): F[Unit] = execute(DELETE(uri / path, token))
  /**
    * Alternative syntax to disable an authentication method:
    * {{{ client.sys.auth -= "my-path" }}}
    */
  def -=(path: String): F[Unit] = disable(path)
  /**
    * Allows disabling multiple authentication methods in one go:
    * {{{
    *   client.sys.auth --= List("my-path", "your-path")
    * }}}
    */
  def --=(paths: List[String]): F[List[Unit]] = paths.map(disable).sequence

  /**
    * Reads the given auth path's configuration.
    *
    * This endpoint requires sudo capability on the final path, but the same functionality can be achieved without sudo via sys/mounts/auth/[auth-path]/tune.
    *
    * @param path Specifies the path in which to tune.
    */
  def tuneOptions(path: String): F[Option[TuneOptions]] =
    for {
      request <- GET(uri / path / "tune", token)
      response <- client.expectOption[Context[TuneOptions]](request)
    } yield response.map(_.data)
  def apply(path: String): F[TuneOptions] = tuneOptions(path).map(_.get)

  /**
    * Tune configuration parameters for a given auth path.
    *
    * This endpoint requires sudo capability on the final path, but the same functionality can be achieved without sudo via sys/mounts/auth/[auth-path]/tune.
    *
    * @param path Specifies the path in which to tune.
    * @param options the tune options to set on path
    */
  def tune(path: String, options: TuneOptions): F[Unit] = execute(POST(options, uri / path / "tune", token))
}