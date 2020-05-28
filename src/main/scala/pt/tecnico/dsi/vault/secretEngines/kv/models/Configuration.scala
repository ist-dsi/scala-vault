package pt.tecnico.dsi.vault.secretEngines.kv.models

import scala.concurrent.duration.{Duration, FiniteDuration}
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decoderFiniteDuration, encodeFiniteDuration}

object Configuration {
  implicit val codec: Codec.AsObject[Configuration] = deriveCodec(renaming.snakeCase, true, None)
}
/**
  * @param maxVersions The number of versions to keep per key. This value applies to all keys, but a key's metadata setting can overwrite this value.
  *                    Once a key has more than the configured allowed versions the oldest version will be permanently deleted. Defaults to 10.
  * @param casRequired If true all keys will require the cas parameter to be set on all write requests.
  * @param deleteVersionAfter If set, specifies the length of time before a version is deleted.
  */
case class Configuration(maxVersions: Int = 10, casRequired: Boolean = false, deleteVersionAfter: FiniteDuration = Duration.Zero)

