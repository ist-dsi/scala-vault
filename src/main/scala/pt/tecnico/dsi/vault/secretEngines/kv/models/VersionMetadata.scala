package pt.tecnico.dsi.vault.secretEngines.kv.models

import java.time.OffsetDateTime
import io.circe.{Codec, HCursor, Json, JsonObject}
import io.circe.derivation.{deriveCodec, renaming}
import io.circe.Decoder.Result

object VersionMetadata {
  private val derivedCodec: Codec.AsObject[VersionMetadata] = deriveCodec(renaming.snakeCase)
  implicit val codec: Codec.AsObject[VersionMetadata] = new Codec.AsObject[VersionMetadata] {
    override def apply(c: HCursor): Result[VersionMetadata] = derivedCodec.prepare(_.withFocus(_.mapObject(_.mapValues(_.withString{
      case s if s.isEmpty => Json.Null
      case s => Json.fromString(s)
    })))).apply(c)

    override def encodeObject(a: VersionMetadata): JsonObject = derivedCodec.encodeObject(a)
  }
}
case class VersionMetadata(createdTime: OffsetDateTime, deletionTime: Option[OffsetDateTime], destroyed: Boolean = false, version: Int = 1)
