package pt.tecnico.dsi.vault.secretEngines.databases.models

import io.circe.Codec
import io.circe.syntax._

trait BaseConnectionObject[C <: BaseConnection] {
  val pluginName: String

  protected val derivedCodec: Codec.AsObject[C]
  implicit lazy val codec: Codec.AsObject[C] = Codec.AsObject.from(derivedCodec, derivedCodec.mapJsonObject(_.add("plugin_name", pluginName.asJson)))
}

trait BaseConnection {
  /** The name of the plugin to use for this connection. */
  def pluginName: String
  /** If the connection is verified during initial configuration. */
  def verifyConnection: Boolean
  /** List of the roles allowed to use this connection. Defaults to empty (no roles), if contains a "*" any role can use this connection. */
  def allowedRoles: Array[String]
  /** The database statements to be executed to rotate the root user's credentials. */
  def rootRotationStatements: Array[String]
}
