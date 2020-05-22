package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.{DSL, VaultClient}
import pt.tecnico.dsi.vault.sys.models.SecretEngine
import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions

final class Mounts[F[_]: Sync: Client](val path: String, val uri: Uri, vaultClient: VaultClient[F])(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists all the mounted secrets engines. */
  def list(): F[Map[String, SecretEngine]] = executeWithContextData(GET(uri, token))

  /**
    * Enables a new secret engine at the given path.
    * @param path Specifies the path where the secrets engine will be mounted.
    * @param engine the secret engine to enable.
    */
  def enable(path: String, engine: SecretEngine): F[Unit] = executeHandlingErrors(POST(engine, uri / path, token)) {
    case errors if errors.exists(_.contains("path is already in use at")) =>
      apply(path).flatMap {
        case options if options == engine.config => implicitly[Sync[F]].unit
        case _ => tune(path, engine.config)
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
  def ++=(list: List[(String, SecretEngine)]): F[Unit] = list.map(+=).sequence_

  /**
    * Enables a new secret engine at the given path and returns the controller for it.
    * @param path Specifies the path where the secrets engine will be mounted.
    * @param engine the secret engine to enable.
    */
  def enableAndReturn(path: String, engine: SecretEngine): F[engine.Out[F]] =
    enable(path, engine).as(engine.mounted(vaultClient, path))

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
  def --=(paths: List[String]): F[Unit] = paths.map(disable).sequence_

  /**
    * Reads the given mount's configuration. Unlike the mounts endpoint, this will return the current time in seconds
    * for each TTL, which may be the system default or a mount-specific value.
    * @param path Specifies the path for the secrets engine.
    * @return
    */
  def tuneOptions(path: String): F[Option[TuneOptions]] = executeOptionWithContextData(GET(uri / path / "tune", token))
  def apply(path: String): F[TuneOptions] = executeWithContextData(GET(uri / path / "tune", token))

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
  def remount(from: String, to: String): F[Unit] =
    execute(POST(Map("from" -> from, "to" -> to).asJson, vaultClient.sys.uri / "remount", token))
}
