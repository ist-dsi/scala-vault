package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.KeyStatus

final class Keys[F[_]: Concurrent: Client](uri: Uri)(implicit token: Header.Raw) {
  private val dsl = new DSL[F] {}
  import dsl._

  /**
    * Triggers a rotation of the backend encryption key. This is the key that is used to encrypt data written to the
    * storage backend, and is not provided to operators. This operation is done online. Future values are encrypted
    * with the new key, while old values are decrypted with previous encryption keys.
    *
    * This path requires sudo capability in addition to update. */
  val rotate: F[Unit] = execute(PUT(uri / "rotate", token))

  /**
    * Returns information about the current encryption key used by Vault.
    */
  val keyStatus: F[KeyStatus] = execute(GET(uri / "key-status"))
}
