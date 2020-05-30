package pt.tecnico.dsi.vault.secretEngines.databases.models.MySQL

import scala.concurrent.duration.Duration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import pt.tecnico.dsi.vault.secretEngines.databases.models.{BaseConnection, BaseConnectionObject}

object Connection extends BaseConnectionObject[Connection] {
  override val pluginName: String = "mysql-database-plugin"
  override protected val derivedCodec: Codec.AsObject[Connection] = deriveCodec(renaming.snakeCase)
}

case class Connection(connectionUrl: String, username: String, password: String,
                      maxOpenConnections: Int = 2, maxIdleConnections: Int = 0, maxConnectionLifetime: Duration = Duration.Zero,
                      verifyConnection: Boolean = true, allowedRoles: Array[String] = Array.empty,
                      rootRotationStatements: Array[String] = Array.empty) extends BaseConnection {
  /** The name of the plugin to use for this connection. */
  override def pluginName: String = Connection.pluginName
}