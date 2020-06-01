package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.Plugin
import pt.tecnico.dsi.vault.sys.models.Plugin.Type

class PluginCatalog[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Lists the plugins in the catalog by type. */
  def list(): F[Map[String, List[String]]] = executeWithContextData(GET(uri, token))

  /** Lists the auth plugins in the catalog. */
  def listAuth(): F[List[String]] = executeWithContextKeys(GET(uri / "auth", token))
  /** Lists the database plugins in the catalog. */
  def listDatabase(): F[List[String]] = executeWithContextKeys(GET(uri / "database", token))
  /** Lists the secret plugins in the catalog. */
  def listSecret(): F[List[String]] = executeWithContextKeys(GET(uri / "secret", token))

  /**
    * Registers a new plugin, or updates an existing one with the supplied name.
    *
    * @param `type` the type of this plugin. May be "auth", "database", or "secret".
    * @param plugin the plugin to register.
    */
  def register(`type`: Type, plugin: Plugin): F[Unit] = execute(PUT(plugin, uri / `type`.toString.toLowerCase / plugin.name, token))

  /**
    * @param name the name of the plugin to retrieve.
    * @param `type` the type of this plugin.
    * @return returns the configuration data for the plugin with the given name.
    */
  def get(`type`: Type, name: String): F[Option[Plugin]] = executeOptionWithContextData(GET(uri / `type`.toString.toLowerCase / name, token))
  def apply(`type`: Type, name: String): F[Plugin] = executeWithContextData(GET(uri / `type`.toString.toLowerCase / name, token))

  /**
    * Removes the plugin with the given name.
    * @param `type` the type of this plugin. May be "auth", "database", or "secret".
    * @param name the name of the plugin to delete.
    */
  def remove(`type`: Type, name: String): F[Unit] = execute(DELETE(uri / `type`.toString.toLowerCase / name, token))
}
