package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import io.circe.derivation.{deriveEncoder, renaming}
import io.circe.Encoder

object Application {
  implicit val encoder: Encoder.AsObject[Application] = deriveEncoder(renaming.snakeCase)
}
/**
  * @param application The name of the application to which this entry applies.
  * @param privileges A list of application privileges or actions.
  * @param resources A list resources to which the privileges are applied.
  */
case class Application(
  application: String,
  privileges: Option[List[String]] = Option.empty,
  resources: Option[List[String]] = Option.empty,
)

