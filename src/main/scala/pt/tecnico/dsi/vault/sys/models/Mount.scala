package pt.tecnico.dsi.vault.sys.models

import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.vault.VaultClient

object Mount {
  def encoder: Encoder.AsObject[Mount] =
    Encoder.forProduct6("type", "description", "config", "options", "local", "seal_wrap") { engine: Mount =>
      (engine.`type`, engine.description, engine.config, engine.options, engine.local, engine.sealWrap)
    }.mapJsonObject { obj =>
      // When tunning (updating) a Mount you can set token_type, however when you are enabling it you cannot.
      // So we simply remove token_type from the encoded json.
      val jsonObject = obj.filterKeys(_ == "config").mapValues(_.mapObject(_.remove("token_type")))
      obj.add("config", jsonObject.values.head)
    }
  def decoder[T <: Mount](
    f: (String, String, TuneOptions, Option[Map[String, String]], Boolean, Boolean) => T): Decoder[T] =
    Decoder.forProduct6("type", "description", "config", "options", "local", "seal_wrap")(f)
}

trait Mount {
  /** Specifies the type of this mount, such as "approle" (authentication method) or "pki" (secret engine). */
  val `type`: String
  /** Specifies a human-friendly description of this mount. */
  val description: String
  /** Specifies configuration options for this mount; if set on a specific mount, values will
    * override any global defaults (e.g. the system TTL/Max TTL) */
  val config: TuneOptions
  /** Specifies mount type specific options that are passed to the backend. */
  val options: Option[Map[String, String]]
  /** Specifies if this is a local mount only. Local mounts are not replicated nor (if a secondary) removed by replication. */
  val local: Boolean
  /** Enable seal wrapping for the mount, causing values stored by the mount to be wrapped by the seal's encryption capability. */
  val sealWrap: Boolean

  type Out[T[_]]
  def mounted[F[_]](vaultClient: VaultClient[F], path: String): Out[F]
}