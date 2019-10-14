package pt.tecnico.dsi.vault.secretEngines.databases.models

import scala.concurrent.duration.FiniteDuration
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import pt.tecnico.dsi.vault.{encodeFiniteDuration, decoderFiniteDuration}

object StaticRole {
  implicit val encoder = deriveEncoder[StaticRole](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[StaticRole](renaming.snakeCase, false, None)
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param username the database username that this Vault role corresponds to.
  * @param rotationPeriod  the amount of time Vault should wait before rotating the password. The minimum is 5 seconds.
  */
case class StaticRole(dbName: String, username: String, rotationPeriod: FiniteDuration)