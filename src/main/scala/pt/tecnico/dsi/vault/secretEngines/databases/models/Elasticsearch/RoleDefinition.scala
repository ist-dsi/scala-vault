package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import io.circe.{Encoder, JsonObject}
import io.circe.derivation.{deriveEncoder, renaming}

object RoleDefinition {
  implicit val encoder: Encoder.AsObject[RoleDefinition] = deriveEncoder(renaming.snakeCase)
}
/**
  *
  * @param cluster A list of cluster privileges. These privileges define the cluster level actions that users with this role are able to execute.
  * @param indices  A list of indices permissions entries.
  * @param applications A list of application privilege entries.
  * @param runAs A list of users that the owners of this role can impersonate. For more information, see
  *              <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/run-as-privilege.html">
  *              Submitting requests on behalf of other users</a>.
  * @param global An object defining global privileges. A global privilege is a form of cluster privilege that is request-aware.
  *               Support for global privileges is currently limited to the management of application privileges. This field is optional.
  * @param metadata Optional meta-data. Within the metadata object, keys that begin with _ are reserved for system usage.
  */
case class RoleDefinition(
  runAs: List[String] = List.empty,
  cluster: List[String] = List.empty,
  global: Option[JsonObject] = Option.empty,
  indices: List[Indice] = List.empty,
  applications: List[Application] = List.empty,
  metadata: Option[JsonObject] = Option.empty
)