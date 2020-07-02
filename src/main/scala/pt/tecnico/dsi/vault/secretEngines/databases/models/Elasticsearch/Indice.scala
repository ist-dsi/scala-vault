package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import io.circe.derivation.{deriveEncoder, renaming}
import io.circe.{Encoder, JsonObject}

object Indice {
  implicit val encoder: Encoder[Indice] = deriveEncoder[Indice](renaming.snakeCase).mapJson(_.deepDropNullValues)
}
/**
  * @param names A list of indices (or index name patterns) to which the permissions in this entry apply.
  * @param privileges The index level privileges that the owners of the role have on the specified indices.
  * @param fieldSecurity he document fields that the owners of the role have read access to. For more information, see
  *                      <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/field-and-document-access-control.html">
  *                      Setting up field and document level security</a>.
  * @param query A search query that defines the documents the owners of the role have read access to.
  *              A document within the specified indices must match this query in order for it to be accessible by the owners of the role.
  */
case class Indice(
  names: List[String],
  privileges: List[String],
  fieldSecurity: Option[JsonObject] = Option.empty,
  query: Option[JsonObject] = Option.empty,
  allowRestrictedIndices: Boolean = false,
)