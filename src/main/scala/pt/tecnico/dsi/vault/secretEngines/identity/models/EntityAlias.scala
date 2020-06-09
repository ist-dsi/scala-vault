package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime
import io.circe.Codec
import io.circe.derivation.deriveCodec

object EntityAlias {
  implicit val codec: Codec[EntityAlias] = deriveCodec(identity)
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
)
