package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.RootGenerationProgress

final class GenerateRoot[F[_]: Sync: Client](val path: String, val uri: Uri) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the configuration and progress of the current root generation attempt. */
  def progress(): F[RootGenerationProgress] = execute(GET(uri / "attempt"))

  /**
    * Start a new root generation attempt. Only a single root generation attempt can take place at a time.
    *
    * @param pgpKey Specifies a base64-encoded PGP public key. The raw bytes of the token will be encrypted with
    *               this value before being returned to the final unseal key provider.
    * @return the configuration and progress of the current root generation attempt.
    */
  def start(pgpKey: Option[String] = None): F[RootGenerationProgress] =
    execute(PUT(Map("pgp_key" -> pgpKey), uri / "attempt"))

  /**
    * Cancels any in-progress root generation attempt. This clears any progress made.
    * This must be called to change the OTP or PGP key being used.
    */
  def cancel(): F[Unit] = execute(DELETE(uri / "attempt"))

  /**
    * This endpoint is used to enter a single master key share to progress the root generation attempt.
    * If the threshold number of master key shares is reached, Vault will complete the root generation and issue
    * the new token. Otherwise, this API must be called multiple times until that threshold is met.
    * The attempt nonce must be provided with each call.
    * @param keyShare Specifies a single master key share.
    * @param nonce Specifies the nonce of the attempt.
    */
  def put(keyShare: String, nonce: String): F[RootGenerationProgress] =
    execute(PUT(Map("key" -> keyShare, "nonce" -> nonce), uri / "update"))
}
