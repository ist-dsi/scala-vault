package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.{BackupKeys, RekeyProgress, RekeyVerificationProgress}

class Rekey[F[_]: Sync](uri: Uri)(implicit client: Client[F]) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return the configuration and progress of the current rekey attempt. */
  def progress(): F[RekeyProgress] = execute(GET(uri / "init"))

  /**
    * Start a new rekey attempt. Only a single rekey attempt can take place at a time.
    *
    * @param shares the number of shares to split the master key into.
    * @param threshold the number of shares required to reconstruct the master key.
    *                        This must be less than or equal to secretShares.
    * @param pgpKeys an array of PGP public keys used to encrypt the output unseal keys. Ordering is preserved.
    *                The keys must be base64-encoded from their original binary representation.
    *                The size of this array must be the same as secretShares.
    * @param backup if using PGP-encrypted keys, whether Vault should also store a plaintext backup of the PGP-encrypted
    *               keys at `core/unseal-keys-backup` in the physical storage backend. These can then be retrieved and
    *               removed via the `sys/rekey/backup` endpoint.
    * @param requireVerification turns on verification functionality. When verification is turned on, after successful
    *                            authorization with the current unseal keys, the new unseal keys are returned but the
    *                            master key is not actually rotated. The new keys must be provided to authorize the
    *                            ctual rotation of the master key. This ensures that the new keys have been successfully
    *                            saved and protects against a risk of the keys being lost after rotation but before they
    *                            can be persisted. This can be used with without pgp_keys, and when used with it, it
    *                            allows ensuring that the returned keys can be successfully decrypted before committing
    *                            to the new shares, which the backup functionality does not provide.
    * @return the configuration and progress of the current rekey attempt.
    */
  def start(shares: Int, threshold: Int, pgpKeys: Option[List[String]] = None, backup: Boolean = false, requireVerification: Boolean = false): F[RekeyProgress] = {
    case class StartSettings(secretShares: Int, secretThreshold: Int, pgpKeys: Option[List[String]], backup: Boolean, requireVerification: Boolean)
    execute(PUT(StartSettings(shares, threshold, pgpKeys, backup, requireVerification).asJson, uri / "init"))
  }

  /**
    * Cancels any in-progress rekey. This clears the rekey settings as well as any progress made.
    * This must be called to change the parameters of the rekey.
    * Note: verification is still a part of a rekey. If rekeying is canceled during the verification flow, the current unseal keys remain valid.
    */
  def cancel(): F[Unit] = execute(DELETE(uri / "init"))

  /**
    * This endpoint is used to enter a single master key share to progress the rekey of the Vault.
    * If the threshold number of master key shares is reached, Vault will complete the rekey.
    * Otherwise, this API must be called multiple times until that threshold is met.
    * The rekey nonce operation must be provided with each call.
    *
    * If verification was requested, successfully completing this flow will immediately put the operation into a
    * verification state, and provide the nonce for the verification operation.
    *
    * @param key Specifies a single master key share.
    * @param nonce Specifies the nonce of the attempt.
    */
  def put(key: String, nonce: String): F[RekeyProgress] =
    execute(PUT(Map("key" -> key, "nonce" -> nonce).asJson, uri / "update"))

  object backupKey {
    val uri: Uri = self.uri / "backup"

    /** @return the backup copy of PGP-encrypted unseal keys. The returned value is the nonce of the rekey operation and
      *          a map of PGP key fingerprint to hex-encoded PGP-encrypted key. */
    def apply(): F[BackupKeys] = execute(GET(uri))
    /** Deletes the backup copy of PGP-encrypted unseal keys. */
    def delete(): F[Unit] = execute(DELETE(uri))
  }

  object verify {
    val uri = self.uri / "verify"

    /** @return the configuration and progress of the current rekey verification attempt. */
    def progress(): F[RekeyVerificationProgress] = execute(GET(uri))
    /**
      * Cancels any in-progress rekey verification operation. This clears any progress made and resets the nonce.
      * Unlike a DELETE against `sys/rekey/init`, this only resets the current verification operation, not the entire rekey atttempt.
      * The return value is the same as GET along with the new nonce. */
    def cancel(): F[RekeyVerificationProgress] = execute(DELETE(uri))
    /**
      * This endpoint is used to enter a single new key share to progress the rekey verification operation.
      * If the threshold number of new key shares is reached, Vault will complete the rekey by performing the actual
      * rotation of the master key. Otherwise, this API must be called multiple times until that threshold is met.
      * The nonce must be provided with each call.
      *
      * @param key a single master share key from the new set of shares.
      * @param nonce the nonce of the rekey verification operation.
      */
    def put(key: String, nonce: String): F[RekeyVerificationProgress] =
      execute(PUT(Map("key" -> key, "nonce" -> nonce).asJson, uri))
  }
}
