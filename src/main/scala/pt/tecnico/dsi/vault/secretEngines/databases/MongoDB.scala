package pt.tecnico.dsi.vault.secretEngines.databases

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB._

class MongoDB[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) extends Databases[F, Connection, Role](uri)