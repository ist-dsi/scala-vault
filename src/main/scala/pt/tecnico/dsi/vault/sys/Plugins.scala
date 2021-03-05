package pt.tecnico.dsi.vault.sys

import cats.effect.Concurrent
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.sys.models.Plugin
import pt.tecnico.dsi.vault.sys.models.Plugin.Type

class Plugins[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header.Raw) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  object catalog {
    val path: String = s"${self.path}/catalog"
    val uri: Uri = self.uri / "catalog"

    /** Lists the plugins in the catalog by type. */
    val list: F[Map[String, List[String]]] = executeWithContextData(GET(uri, token))

    /** Lists the auth plugins in the catalog. */
    val listAuth: F[List[String]] = executeWithContextKeys(GET(uri / "auth", token))
    /** Lists the database plugins in the catalog. */
    val listDatabase: F[List[String]] = executeWithContextKeys(GET(uri / "database", token))
    /** Lists the secret plugins in the catalog. */
    val listSecret: F[List[String]] = executeWithContextKeys(GET(uri / "secret", token))

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

  /**
    * Reload mounted plugin backends.
    *
    * @param plugin The name of the plugin to reload, as registered in the plugin catalog.
    */
  def reload(plugin: String): F[Unit] = reload(plugin, scope = None)

  /**
    * Reload mounted plugin backends.
    *
    * @param plugin The name of the plugin to reload, as registered in the plugin catalog.
    * @param scope The scope of the reload. If ommitted, reloads the plugin or mounts on this Vault instance.
    *              If 'global', will begin reloading the plugin on all instances of a cluster.
    */
  def reload(plugin: String, scope: Option[String]): F[Unit] =
    execute(PUT(Map("plugin" -> plugin, "scope" -> scope.getOrElse("")).asJson, uri / "reload" / "backend", token))

  /**
    * Reload mounted plugin backends.
    *
    * @param mounts Array or comma-separated string mount paths of the plugin backends to reload.
    * @param scope The scope of the reload. If ommitted, reloads the plugin or mounts on this Vault instance.
    *              If 'global', will begin reloading the plugin on all instances of a cluster.
    */
  def reload(mounts: List[String], scope: Option[String] = None): F[Unit] =
    execute(PUT(Map("mounts" -> mounts.asJson, "scope" -> scope.getOrElse("").asJson), uri / "reload" / "backend", token))
}
