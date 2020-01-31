package pt.tecnico.dsi.vault

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.Decoder
import org.http4s.Status.Successful
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.impl.Methods
import org.http4s.{EntityDecoder, Request, Status}

abstract class DSL[F[_]](implicit client: Client[F], F: Sync[F]) extends Http4sClientDsl[F] with Methods {
  type EntityDecoderF[T] = EntityDecoder[F, T]

  /**
    * Executes a request for an endpoint which returns `Context[Data]` but we are only interested in its data.
    * @param request the request for the endpoint
    * @param decoder EntityDecoder for `Context[Data]`
    * @tparam Data the type we are interested in.
    */
  def executeWithContextData[Data: Decoder](request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Data]]): F[Data] =
    execute[Context[Data]](request).map(_.data)

  /**
    * Executes a request for an endpoint which returns `Context[Keys]` but we are only interested in the keys.
    * @param request the request for the endpoint
    * @param decoder EntityDecoder for `Context[Keys]`
    */
  def executeWithContextKeys(request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Keys]]): F[List[String]] =
    execute[Context[Keys]](request).map(_.data.keys)

  /**
    * Executes a request for an endpoint which returns `Context[A]` but we are only interested in the `Auth`.
    * @param request the request for the endpoint
    */
  def executeWithContextAuth(request: F[Request[F]]): F[Auth] = {
    // TODO: how to not use .get?
    execute[Context[Option[Unit]]](request).map(_.auth.get)
  }

  /**
    * Executes a request, handling any `BadRequest` returned by Vault by decoding it to `Errors` and then raising an
    * error in F.
    * @param request the request to execute.
    * @tparam A the type to which the response will be decoded to.
    */
  def execute[A: Decoder: EntityDecoderF](request: F[Request[F]]): F[A] =
    executeHandlingErrors(request)(PartialFunction.fromFunction(raise[A]))

  type ?=>[T, R] = PartialFunction[T, R]

  /**
    * Execute the given `request`. On any success (2XX) decode the body to an `A`.
    * If a `BadRequest` is returned its body will be decoded to a `List[String]` and
    * the `onErrorPF` will be invoked. Allowing to recover for some errors.
    * Any other type of HTTP error (4XX and 5XX) will raise error with `UnexpectedStatus`.
    * @param request the request to execute.
    * @param onErrorsPF the `PartialFunction` to apply on a BadRequest.
    * @tparam A the type to decode the response into.
    */
  def executeHandlingErrors[A: Decoder: EntityDecoderF](request: F[Request[F]])(onErrorsPF: List[String] ?=> F[A]): F[A] = {
    client.fetch(request) {
      case Successful(response) => response.as[A]
      case failedResponse => failedResponse.status match {
        case Status.BadRequest =>
          failedResponse.as[Errors].flatMap(errors => onErrorsPF.applyOrElse(errors.errors, raise))
        case status =>
          F.raiseError(UnexpectedStatus(status))
      }
    }
  }

  def executeOptionHandlingErrors[A: Decoder: EntityDecoderF](request: F[Request[F]])
                                                             (onErrorsPF: List[String] ?=> F[Option[A]]): F[Option[A]] =
    client.fetch(request) {
      case Successful(response) => response.as[A].map(_.some)
      case failedResponse =>
        failedResponse.status match {
          case Status.BadRequest =>
            failedResponse.as[Errors].flatMap(errors => onErrorsPF.applyOrElse(errors.errors, raise))
          case Status.NotFound | Status.Gone => Option.empty[A].pure[F]
          case status =>
            F.raiseError(UnexpectedStatus(status))
        }
    }

  private def raise[A](errors: List[String]): F[A] = F.raiseError(ErroredRequest(errors))
}