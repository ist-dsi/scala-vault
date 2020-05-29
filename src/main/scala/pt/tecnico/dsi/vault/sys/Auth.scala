package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.{DSL, VaultClient}
import pt.tecnico.dsi.vault.sys.models.AuthMethod
import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions

/**
  * @define sudoRequired <b>sudo required</b> – This endpoint requires sudo capability in addition to any path-specific capabilities.
  */
final class Auth[F[_]: Sync: Client](val path: String, val uri: Uri, vaultClient: VaultClient[F])(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists all enabled auth methods. */
  def list(): F[Map[String, AuthMethod]] = executeWithContextData(GET(uri, token))

  /**
    * Enables a new auth method. After enabling, the auth method can be accessed and configured via the auth path
    * specified as part of the URL. This auth path will be nested under the `auth` prefix. For example, enable the "foo"
    * auth method will make it accessible at /auth/foo.
    *
    * $sudoRequired
    *
    * @param path Specifies the path in which to enable the auth method.
    * @param method the authentication method to enable.
    */
  def enable(path: String, method: AuthMethod): F[Unit] = executeHandlingErrors(POST(method, uri / path, token)) {
    case errors if errors.exists(_.contains("path is already in use at")) =>
      apply(path).flatMap {
        case config if config == method.config => implicitly[Sync[F]].unit
        case _ => tune(path, method.config)
      }
  }

  /**
    * Enables a new auth method and returns the controller for it.
    *
    * $sudoRequired
    *
    * @param path Specifies the path in which to enable the auth method.
    * @param method the authentication method to enable.
    */
  def enableAndReturn(path: String, method: AuthMethod): F[method.Out[F]] = enable(path, method).as(method.mounted(vaultClient, path))

  /**
    * Disables the auth method at the given auth path.
    *
    * $sudoRequired
    *
    * @param path Specifies the path to disable.
    */
  def disable(path: String): F[Unit] = execute(DELETE(uri / path, token))

  /**
    * Reads the given auth path's configuration.
    *
    * This endpoint requires sudo capability on the final path, but the same functionality can be achieved without sudo via sys/mounts/auth/[auth-path]/tune.
    *
    * @param path Specifies the path in which to tune.
    */
  def tuneOptions(path: String): F[Option[TuneOptions]] = executeOptionWithContextData(GET(uri / path / "tune", token))
  def apply(path: String): F[TuneOptions] = executeWithContextData(GET(uri / path / "tune", token))

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
