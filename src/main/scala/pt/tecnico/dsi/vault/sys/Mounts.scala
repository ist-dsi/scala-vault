package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.VaultClient
import pt.tecnico.dsi.vault.sys.models.SecretEngine

/** Handles mounting secret engines. For auth methods look at Auth. */
final class Mounts[F[_]: Concurrent: Client](path: String, uri: Uri, vaultClient: VaultClient[F])(implicit token: Header)
  extends MountService[F, SecretEngine](path, uri, vaultClient)
