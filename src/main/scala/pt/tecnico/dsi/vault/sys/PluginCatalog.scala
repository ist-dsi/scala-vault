package pt.tecnico.dsi.vault.sys

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.foldable._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.sys.models.Plugin
import pt.tecnico.dsi.vault.sys.models.Plugin.Type

class PluginCatalog[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
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
  def register(`type`: Type, plugin: Plugin): F[Unit] =
    execute(PUT(plugin, uri / `type`.toString.toLowerCase / plugin.name, token))
  /**
    * Alternative syntax to register a plugin:
    * {{{ client.sys.pluginCatalog += Plugin.Auth -> Plugin(...) }}}
    */
  def +=(tuple: (Type, Plugin)): F[Unit] = register(tuple._1, tuple._2)
  /**
    * Allows registering multiple plugins in one go:
    * {{{
    *   client.sys.pluginCatalog ++= List(
    *     Plugin.Auth-> Plugin(...),
    *     Plugin.Database -> Plugin(...),
    *   )
    * }}}
    */
  def ++=(list: List[(Type, Plugin)]): F[Unit] = list.map(+=).sequence_

  def apply(`type`: Type, name: String): F[Plugin] = get(`type`, name).map(_.get)
  /**
    * @param name the name of the plugin to retrieve.
    * @param `type` the type of this plugin.
    * @return returns the configuration data for the plugin with the given name.
    */
  def get(`type`: Type, name: String): F[Option[Plugin]] =
    for {
      request <- GET(uri / `type`.toString.toLowerCase / name, token)
      response <- client.expectOption[Context[Plugin]](request)
    } yield response.map(_.data)

  /**
    * Removes the plugin with the given name.
    * @param `type` the type of this plugin. May be "auth", "database", or "secret".
    * @param name the name of the plugin to delete.
    */
  def remove(`type`: Type, name: String): F[Unit] = execute(DELETE(uri / `type`.toString.toLowerCase / name, token))
  /**
    * Alternative syntax to remove a plugin from the catalog:
    * {{{ client.sys.pluginCatalog -= (Plugin.Auth, "plugin-name") }}}
    */
  def -=(tuple: (Type, String)): F[Unit] = remove(tuple._1, tuple._2)
  /**
    * Allows removing multiple plugin from the catalog in one go:
    * {{{
    *   client.sys.pluginCatalog --= List(Plugin.Auth -> "auth-plugin-name", Plugin.Database -> "mysql")
    * }}}
    */
  def --=(plugins: List[(Type, String)]): F[Unit] = plugins.map(-=).sequence_
}
