package pt.tecnico.dsi.vault

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.Status.{BadRequest, ClientError, Gone, NotFound, Successful, ServerError}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.impl.EmptyRequestGenerator
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.impl.Methods
import org.http4s.{circe, EntityDecoder, EntityEncoder, Method, Request}

abstract class DSL[F[_]](implicit client: Client[F], F: Sync[F]) extends Http4sClientDsl[F] with Methods {
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf[F, A](jsonPrinter)
  implicit def jsonDecoder[A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf[F, A]
  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit def void: EntityDecoder[F, Unit] = EntityDecoder.void

  /** Creates a new `Decoder[A]` by first going down to `field`. */
  def decoderDownField[A](field: String)(implicit decoder: Decoder[A]): Decoder[A] = decoder.prepare(_.downField(field))

  // Have you ever heard about the LIST HTTP method, neither have I, as it does not exist </faceplam>
  // Unfortunately NoBody is a sealed trait. So we must resort to a more verbose, and ugly, way of getting a nice syntax for LIST.
  // We need a marker type to ensure we are not generating an EmptyRequestGenerator for methods with body.
  sealed trait Marker
  val LIST: Method with Marker = Method.fromString("LIST").toOption.get.asInstanceOf[Method with Marker]
  implicit def listOps(list: Method with Marker): EmptyRequestGenerator[F] = new EmptyRequestGenerator[F] {
    override def method: Method = list
  }

  type EntityDecoderF[T] = EntityDecoder[F, T]

  /**
    * Executes a request for an endpoint which returns `Context[Data]` but we are only interested in its data.
    * @param request the request for the endpoint
    * @tparam Data the type we are interested in.
    */
  def executeWithContextData[Data : Decoder](request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Data]]): F[Data] =
    execute[Context[Data]](request).map(_.data)

  def executeOptionWithContextData[Data: Decoder](request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Data]]): F[Option[Data]] =
    executeOption[Context[Data]](request).map(_.map(_.data))

  /**
    * Executes a request for an endpoint which returns `Context[Keys]` but we are only interested in the keys.
    * @param request the request for the endpoint
    */
  def executeWithContextKeys(request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Keys]]): F[List[String]] =
    execute[Context[Keys]](request).map(_.data.keys)

  /**
    * Executes a request for an endpoint which returns `Context[_]` but we are only interested in the `Auth`.
    * @param request the request for the endpoint
    */
  def executeWithContextAuth(request: F[Request[F]]): F[Auth] = execute[Context[Option[Unit]]](request).map(_.auth.get) // TODO: how to not use .get?

  /**
    * Executes a request, handling any `BadRequest` returned by Vault by decoding it to `Errors` and then raising an
    * error in F.
    * @param request the request to execute.
    * @tparam A the type to which the response will be decoded to.
    */
  def execute[A: Decoder: EntityDecoderF](request: F[Request[F]]): F[A] = executeHandlingErrors(request)(PartialFunction.empty)
  /**
    * Executes a request, handling any `BadRequest` returned by Vault by decoding it to `Errors` and then raising an
    * error in F.
    * @param request the request to execute.
    * @tparam A the type to which the response will be decoded to.
    */
  def executeOption[A: Decoder: EntityDecoderF](request: F[Request[F]]): F[Option[A]] = executeOptionHandlingErrors(request)(PartialFunction.empty)

  type ?=>[T, R] = PartialFunction[T, R]

  //TODO: refactor these two methods

  /**
    * Execute the given `request`. On a successful response decode the body to an `A`.
    * If a `BadRequest` is returned its body will be decoded to a `List[String]` and
    * the `onErrorPF` will be invoked. Allowing to recover for some errors.
    * Client or server errors will raise error with `UnexpectedStatus`.
    * @param request the request to execute.
    * @param onErrorsPF the `PartialFunction` to apply on a BadRequest.
    * @tparam A the type to decode the response into.
    */
  def executeHandlingErrors[A: Decoder: EntityDecoderF](request: F[Request[F]])(onErrorsPF: List[String] ?=> F[A]): F[A] =
    client.fetch(request) {
      case Successful(response) => response.as[A]
      case BadRequest(response) => response.as[Errors].flatMap(errors => onErrorsPF.applyOrElse(errors.errors, raise))
      case response @ (ClientError(_) | ServerError(_)) => F.raiseError(UnexpectedStatus(response.status))
    }

  def executeOptionHandlingErrors[A: Decoder: EntityDecoderF](request: F[Request[F]])(onErrorsPF: List[String] ?=> F[Option[A]]): F[Option[A]] =
    client.fetch(request) {
      case Successful(response) => response.as[A].map(_.some)
      case BadRequest(response) => response.as[Errors].flatMap(errors => onErrorsPF.applyOrElse(errors.errors, raise))
      case NotFound(_) | Gone(_) => Option.empty[A].pure[F]
      case response @ (ClientError(_) | ServerError(_)) => F.raiseError(UnexpectedStatus(response.status))
    }

  private def raise[A](errors: List[String]): F[A] = F.raiseError(ErroredRequest(errors))
}