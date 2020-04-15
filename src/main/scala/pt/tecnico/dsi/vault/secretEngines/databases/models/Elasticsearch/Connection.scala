package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.secretEngines.databases.models.{BaseConnection, BaseConnectionObject}

object Connection extends BaseConnectionObject[Connection] {
  override val pluginName: String = "elasticsearch-database-plugin"
  override protected val derivedCodec: Codec.AsObject[Connection] = deriveCodec(renaming.snakeCase, false, None)
}

/**
  * @param url The URL for Elasticsearch's API (eg: "http://localhost:9200").
  * @param username The username to be used in the connection URL.
  * @param password The password to be used in the connection URL.
  * @param caCert The path to a PEM-encoded CA cert file to use to verify the Elasticsearch server's identity.
  * @param caPath The path to a directory of PEM-encoded CA cert files to use to verify the Elasticsearch server's identity.
  * @param clientCert The path to the certificate for the Elasticsearch client to present for communication.
  * @param clientKey The path to the key for the Elasticsearch client to use for communication.
  * @param tlsServerName This, if set, is used to set the SNI host when connecting via TLS.
  * @param insecure If set to true SSL verification will be disabled.
  * @param verifyConnection Specifies if the connection is verified during initial configuration. Defaults to true.
  * @param allowedRoles List of the roles allowed to use this connection. Defaults to empty (no roles), if contains a "*" any role can use this connection.
  */
case class Connection(url: String, username: String, password: String, caCert: String, caPath: String,
                      clientCert: String, clientKey: String, tlsServerName: String, insecure: Boolean = false,
                      verifyConnection: Boolean = true, allowedRoles: Array[String] = Array.empty) extends BaseConnection {

  /** The elasticsearch plugin does not support root rotation statements */
  final val rootRotationStatements: Array[String] = Array.empty

  /** The name of the plugin to use for this connection. */
  override def pluginName: String = Connection.pluginName
}
