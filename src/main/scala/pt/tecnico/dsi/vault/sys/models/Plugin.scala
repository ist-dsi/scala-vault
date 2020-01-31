package pt.tecnico.dsi.vault.sys.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}

object Plugin {
  object Type {
    // A little ugly :(
    implicit val encoder: Encoder[Type] = deriveEnumerationEncoder[Type].mapJson(_.mapString(_.toLowerCase))
    implicit val decoder: Decoder[Type] = deriveEnumerationDecoder[Type].prepare(_.withFocus(_.mapString(_.capitalize)))
  }
  sealed trait Type
  case object Auth extends Type
  case object Database extends Type
  case object Secret extends Type

  implicit val encoder: Encoder[Plugin] = deriveEncoder(renaming.snakeCase, None)
  implicit val decoder: Decoder[Plugin] = deriveDecoder(renaming.snakeCase, false, None)
}

/**
  * @param name the name is what is used to look up plugins in the catalog.
  * @param sha256 the SHA256 sum of the plugin's binary. Before a plugin is run it's SHA will be checked against this value,
  *               if they do not match the plugin can not be run.
  * @param command the command used to execute the plugin. This is relative to the plugin directory. e.g. "myplugin".
  * @param args the arguments used to execute the plugin. If the arguments are provided here, the command parameter
  *             should only contain the named program. e.g. "--my_flag=1".
  * @param env the environment variables used during the execution of the plugin.
  *            Each entry is of the form "key=value". e.g "FOO=BAR".
  */
case class Plugin(name: String, sha256: String, command: String, builtin: Boolean = false,
                  args: Array[String] = Array.empty, env: Array[String] = Array.empty)
