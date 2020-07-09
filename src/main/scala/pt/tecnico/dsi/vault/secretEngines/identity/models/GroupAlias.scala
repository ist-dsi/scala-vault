package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object GroupAlias {
  implicit val decoder: Decoder[GroupAlias] = deriveDecoder(renaming.snakeCase)
}
case class GroupAlias(
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