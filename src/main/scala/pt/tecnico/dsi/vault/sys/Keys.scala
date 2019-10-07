package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.KeyStatus

class Keys[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /**
    * Triggers a rotation of the backend encryption key. This is the key that is used to encrypt data written to the
    * storage backend, and is not provided to operators. This operation is done online. Future values are encrypted
    * with the new key, while old values are decrypted with previous encryption keys.
    *
    * This path requires sudo capability in addition to update. */
  def rotate(): F[Unit] = execute(PUT(uri / "rotate", token))

  /**
    * Returns information about the current encryption key used by Vault.
    */
  def keyStatus(): F[KeyStatus] = execute(GET(uri / "key-status"))
}
