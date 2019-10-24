package pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL

import scala.concurrent.duration.Duration
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.syntax._
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseConnection
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}

// TODO: find a better way to implement the plugin name field.

object Connection {
  final val pluginName: String = "mysql-database-plugin"

  implicit val encoder =
    deriveEncoder[Connection](renaming.snakeCase, None).mapJsonObject(_.add("plugin_name", pluginName.asJson))
  implicit val decoder = deriveDecoder[Connection](renaming.snakeCase, false, None)
}

case class Connection(connectionUrl: String, username: String, password: String,
                      maxOpenConnections: Int = 2, maxIdleConnections: Int = 0, maxConnectionLifetime: Duration = Duration.Zero,
                      verifyConnection: Boolean = true, allowedRoles: Array[String] = Array.empty,
                      rootRotationStatements: Array[String] = Array.empty) extends BaseConnection {
  /** The name of the plugin to use for this connection. */
  override def pluginName: String = Connection.pluginName
}