package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object EntityAlias {
  implicit val decoder: Decoder[EntityAlias] = deriveDecoder(renaming.snakeCase)
}
case class EntityAlias (
  id: String,
  canonicalId: String,
  name: String,
  creationTime: OffsetDateTime,
  lastUpdateTime: OffsetDateTime,
  mountAccessor: String,
  mountPath: String,
  mountType: String,
  metadata: Map[String, String] = Map.empty,
  mergedFromCanonicalIds: List[String] = List.empty,
) extends Alias
