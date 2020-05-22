package pt.tecnico.dsi.vault.secretEngines.kv.models

import java.time.OffsetDateTime
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Version {
  implicit val codec: Codec.AsObject[Version] = deriveCodec(renaming.snakeCase, false, None)
}
case class Version(createdTime: OffsetDateTime, deletionTime: Option[OffsetDateTime], destroyed: Boolean)
