package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Concurrent
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch.*

final class Elasticsearch[F[_]: Concurrent: Client](path: String, uri: Uri)(implicit token: Header.Raw)
  extends Databases[F, Connection, Role](path, uri) with StaticRoles[F] with RootRotation[F]