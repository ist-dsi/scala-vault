package pt.tecnico.dsi.vault.secretEngines.kv.models

import java.time.OffsetDateTime
import io.circe.{Codec, HCursor, Json, JsonObject}
import io.circe.derivation.{deriveCodec, renaming}
import io.circe.Decoder.Result

object Version {
  private val derivedCodec: Codec.AsObject[Version] = deriveCodec(renaming.snakeCase, false, None)
  implicit val codec: Codec.AsObject[Version] = new Codec.AsObject[Version] {
    override def apply(c: HCursor): Result[Version] = derivedCodec.prepare(_.withFocus(_.mapObject(_.mapValues(_.withString{
      case s if s.isEmpty => Json.Null
      case s => Json.fromString(s)
    })))).apply(c)

    override def encodeObject(a: Version): JsonObject = derivedCodec.encodeObject(a)
  }
}
case class Version(createdTime: OffsetDateTime, deletionTime: Option[OffsetDateTime], destroyed: Boolean)
