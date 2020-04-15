package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL._

class MySql[F[_]: Sync](val path: String, uri: Uri)(implicit client: Client[F], token: Header)
  extends Databases[F, Connection, Role](uri) with StaticRoles[F] with RootRotation[F]