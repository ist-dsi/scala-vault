package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.generic.auto._
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.{InitOptions, InitResult}

class Init[F[_]: Sync](uri: Uri)(implicit client: Client[F]) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the initialization status of Vault. */
  def initialized(): F[Boolean] = {
    case class Status(initialized: Boolean)
    execute[Status](GET(uri)).map(_.initialized)
  }

  /**
    * This endpoint initializes Vault. The Vault must not have been previously initialized.
    * The recovery options, as well as the stored shares option, are only available when using Vault HSM.
    *
    * @param options The options to use when initializing Vault.
    * @return The result of initializing Vault.
    */
  def initialize(options: InitOptions): F[InitResult] = execute(PUT(options, uri))
}
