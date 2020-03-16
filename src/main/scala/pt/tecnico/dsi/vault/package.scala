package pt.tecnico.dsi

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Uri

package object vault {
  implicit class RichUri(val uri: Uri) extends AnyVal {
    def append(pathParts: List[String]): Uri = {
      val encoded = pathParts.collect{ case s if s.nonEmpty => Uri.pathEncode(s) }.mkString("/")
      val newPath =
        if (uri.path.isEmpty || uri.path.last != '/') s"${uri.path}/$encoded"
        else s"${uri.path}$encoded"
      uri.withPath(newPath)
    }
    def append(path: Uri.Path): Uri = append(path.split('/').toList)
  }

  implicit val encodeFiniteDuration: Encoder[FiniteDuration] = Encoder.encodeString.contramap(d => s"${d.toSeconds.toString}s")
  implicit val decoderFiniteDuration: Decoder[FiniteDuration] = Decoder.decodeLong.emap(l => Right(FiniteDuration(l, TimeUnit.SECONDS)))

  implicit val encodeDuration: Encoder[Duration] = Encoder.encodeString.contramap(d => if (d eq Duration.Undefined) "" else s"${d.toSeconds.toString}s")
  implicit val decoderDuration: Decoder[Duration] = Decoder.decodeLong.emap(l => Right(Duration(l, TimeUnit.SECONDS)))

  implicit val encodeArrayAsCSV: Encoder[Array[String]] = (a: Array[String]) => Json.fromString(a.mkString(","))
  implicit val decodeArrayAsCSV: Decoder[Array[String]] = Decoder.decodeString.map(_.split(","))
}
