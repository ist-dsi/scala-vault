package pt.tecnico.dsi

import cats.implicits._
import cats.Applicative
import cats.effect.{Bracket, Sync}
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.{EntityDecoder, EntityEncoder, Request}
import org.http4s.circe._
import org.http4s.client.{Client, UnexpectedStatus}

package object vault {
  implicit def jsonDecoder[F[_]: Sync, A](implicit decoder: Decoder[A]): EntityDecoder[F, A] = jsonOf[F, A]
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[F[_]: Applicative, A](implicit encoder: Encoder[A]): EntityEncoder[F, A] =
    jsonEncoderWithPrinterOf[F, A](jsonPrinter)

  implicit class RichClient[F[_]](val client: Client[F]) extends AnyVal {
    def expectUnit(request: F[Request[F]])(implicit F: Bracket[F, Throwable]): F[Unit] =
      client.fetch(request) { r =>
        if (r.status.isSuccess) {
          F.pure(())
        } else {
          F.pure(UnexpectedStatus(r.status)).flatMap(F.raiseError)
        }
      }
  }
}
