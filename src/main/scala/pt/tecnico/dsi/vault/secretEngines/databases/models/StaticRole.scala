package pt.tecnico.dsi.vault.secretEngines.databases.models

import scala.concurrent.duration.FiniteDuration
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.vault.{decoderFiniteDuration, encodeFiniteDuration}

object StaticRole {
  implicit val codec: Codec.AsObject[StaticRole] = deriveCodec(renaming.snakeCase)
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param username the database username that this Vault role corresponds to.
  * @param rotationPeriod  the amount of time Vault should wait before rotating the password. The minimum is 5 seconds.
  */
case class StaticRole(dbName: String, username: String, rotationPeriod: FiniteDuration)