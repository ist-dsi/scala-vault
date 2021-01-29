package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.Method.{GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.{SealStatus, UnsealOptions}

final class Seal[F[_]: Concurrent: Client](uri: Uri) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the seal status of the Vault. */
  def status(): F[SealStatus] = execute(GET(uri / "seal-status"))

  /**
    * Seals the Vault. In HA mode, only an active node can be sealed. Standby nodes should be restarted to get the
    * same effect. Requires a token with `root` policy or `sudo` capability on the path.
    */
  def seal(): F[Unit] = execute(PUT(uri / "seal"))

  /**
    * This endpoint is used to enter a single master key share to progress the unsealing of the Vault.
    * If the threshold number of master key shares is reached, Vault will attempt to unseal the Vault.
    * Otherwise, this API must be called multiple times until that threshold is met.
    *
    * @param key the unseal key to use.
    * @return the current seal status of Vault.
    */
  def unseal(key: String): F[SealStatus] = unseal(UnsealOptions(key))

  /**
    * This endpoint is used to enter a single master key share to progress the unsealing of the Vault.
    * If the threshold number of master key shares is reached, Vault will attempt to unseal the Vault.
    * Otherwise, this API must be called multiple times until that threshold is met.
    *
    * Either the `key` or `reset` parameter must be provided; if both are provided, `reset` takes precedence.
    *
    * @param options the options to use for the unseal.
    */
  def unseal(options: UnsealOptions): F[SealStatus] = execute(PUT(options, uri / "unseal"))
}
