package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.AuthMethod

/**
  * These endpoints require sudo capability in addition to any path-specific capabilities.
  * However the same functionality can be achieved without sudo via the Mounts service.
  */
final class Auth[F[_]: Sync: Client](path: String, uri: Uri, vaultClient: VaultClient[F])(implicit token: Header)
  extends MountService[F, AuthMethod](path, uri, vaultClient) {
  /** @inheritdoc */
  override def remount(from: String, to: String): F[Unit] = super.remount(s"auth/$from", s"auth/$to")
}