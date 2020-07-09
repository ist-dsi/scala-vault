package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Codec
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST}
import org.http4s.Status.Successful
import pt.tecnico.dsi.vault.{DSL, VaultClient}
import pt.tecnico.dsi.vault.sys.models.{Mount, Mounted, TuneOptions}

abstract class MountService[F[_]: Sync: Client, T <: Mount: Codec](val path: String, val uri: Uri, vaultClient: VaultClient[F])(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists all the mounts. */
  def list(): F[Map[String, Mounted]] = executeWithContextData(GET(uri, token))

  /**
    * Enables a new Mount at the given path.
    * @param path Specifies the path where the mount will be mounted.
    * @param mount the Mount to enable.
    */
  def enable(path: String, mount: T): F[Unit] =
    genericExecute(POST(mount, uri / path, token))({
      case Successful(response) => response.as[Unit]
    }, {
      case errors if errors.exists(_.contains("path is already in use at")) =>
        apply(path).flatMap {
          case options if options == mount.config => implicitly[Sync[F]].unit
          case _ => tune(path, mount.config)
        }
    })

  /**
    * Enables a new Mount at the given path and returns the controller for it.
    * @param path Specifies the path where the Mount will be mounted.
    * @param mount the Mount to enable.
    */
  def enableAndReturn(path: String, mount: T): F[mount.Out[F]] = enable(path, mount).as(mount.mounted(vaultClient, path))

  /**
    * Disables the mount point at `path`.
    *
    * @param path Specifies the path of the Mount that will be disabled.
    */
  def disable(path: String): F[Unit] = execute(DELETE(uri / path, token))

  /**
    * Reads the given mount's configuration.
    * @param path Specifies the path for the secrets engine.
    * @return
    */
  def tuneOptions(path: String): F[Option[TuneOptions]] = executeOptionWithContextData(GET(uri / path / "tune", token))
  def apply(path: String): F[TuneOptions] = executeWithContextData(GET(uri / path / "tune", token))

  /**
    * Tunes configuration parameters for a given mount point.
    * @param path Specifies the path for the Mount.
    * @param options the Mount tune options to tune.
    */
  def tune(path: String, options: TuneOptions): F[Unit] = execute(POST(options, uri / path / "tune", token))


  /**
    * Moves an already-mounted backend to a new mount point.
    * @param from Specifies the previous mount point.
    * @param to Specifies the new destination mount point.
    */
  def remount(from: String, to: String): F[Unit] = execute(POST(Map("from" -> from, "to" -> to), vaultClient.sys.uri / "remount", token))
}
