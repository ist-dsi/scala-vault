package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL._

final class MySql[F[_]: Sync: Client](path: String, uri: Uri)(implicit token: Header) extends Databases[F, Connection, Role](path, uri)
  with StaticRoles[F] with RootRotation[F]