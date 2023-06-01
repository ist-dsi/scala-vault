package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import io.circe.Decoder
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.Method.{GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.{InitOptions, InitResult}

final class Init[F[_]: Concurrent: Client](val path: String, val uri: Uri) {
  private val dsl = new DSL[F] {}
  import dsl.*

  /** @return the initialization status of Vault. */
  val initialized: F[Boolean] = {
    implicit val d = Decoder[Boolean].at("initialized")
    execute(GET(uri))
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
