package pt.tecnico.dsi.vault

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.Status.{BadRequest, Gone, NotFound, Successful}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.impl.EmptyRequestGenerator
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{circe, EntityDecoder, EntityEncoder, Method, Request, Response}

abstract class DSL[F[_]](implicit client: Client[F], F: Sync[F]) extends Http4sClientDsl[F] {
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf[F, A](jsonPrinter)
  implicit def jsonDecoder[A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf[F, A]
  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit def void: EntityDecoder[F, Unit] = EntityDecoder.void

  // Have you ever heard about the LIST HTTP method, neither have I, as it does not exist </faceplam>
  // Unfortunately NoBody is a sealed trait. So we must resort to a more verbose, and ugly, way of getting a nice syntax for LIST.
  // We need a marker type to ensure we are not generating an EmptyRequestGenerator for methods with body.
  sealed trait Marker
  val LIST: Method with Marker = Method.fromString("LIST").toOption.get.asInstanceOf[Method with Marker]
  import scala.language.implicitConversions
  implicit def listOps(list: Method with Marker): EmptyRequestGenerator[F] = new EmptyRequestGenerator[F] {
    override def method: Method = list
  }

  type EntityDecoderF[T] = EntityDecoder[F, T]

  /**
    * Executes a request for an endpoint which returns `Context[Data]` but we are only interested in its data.
    * @param request the request for the endpoint
    * @tparam Data the type we are interested in.
    */
  def executeWithContextData[Data: Decoder](request: F[Request[F]])(implicit decoder: EntityDecoder[F, Context[Data]]): F[Data] =
    execute[Context[Data]](request).map(_.data)

  /**
    * Executes a request for an endpoint which returns `Option[Context[Data]]` but we are only interested in the Context data.
    * @param request the request for the endpoint
    * @tparam Data the type we are interested in.
    */
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
  def executeWithContextAuth(request: F[Request[F]]): F[Auth] = execute[Context[Option[Unit]]](request).flatMap { context =>
    F.fromOption(context.auth, new Throwable("Was expecting response to contain \"auth\" field."))
  }

  /**
    * Executes a request, and on a successful response decodes the body to an `A`.
    * On any other response `defaultErrorHandler` will be called.
    *
    * @param request the request to execute.
    * @tparam A the type to which the response will be decoded to.
    */
  def execute[A: Decoder: EntityDecoderF](request: F[Request[F]]): F[A] =
    genericExecute(request) {
      case Successful(response) => response.as[A]
    }

  /**
    * Executes a request, and on a successful response decodes the body to an `A` inside a Some.
    * On a NotFound or Gone response returns a None.
    * On any other response `defaultErrorHandler` will be called.
    *
    * @param request the request to execute.
    * @tparam A the type to which the response will be decoded to.
    */
  def executeOption[A: Decoder: EntityDecoderF](request: F[Request[F]]): F[Option[A]] =
    genericExecute(request) {
      case Successful(response) => response.as[A].map(Option.apply)
      case NotFound(_) | Gone(_) => Option.empty[A].pure[F]
    }

  // Dotty seems intent on ruining this very nice type alias. Why is it nice? Have a look here https://youtu.be/n7PsbJwVSuE?t=409
  type ?=>[T, R] = PartialFunction[T, R]

  /**
    * Execute the given `request`, and apply the partial function `f` to the response.
    * If `f` is not defined for some response the `defaultErrorHandler` will be called with `onErrorsPF`
    *
    * @param request the request to execute.
    * @param onErrorsPF the `PartialFunction` to apply on the BadRequest errors.
    */
  def genericExecute[A](request: F[Request[F]])(f: Response[F] ?=> F[A], onErrorsPF: List[String] ?=> F[A] = PartialFunction.empty): F[A] =
    request.flatMap(client.run(_).use(f.applyOrElse(_, defaultErrorHandler(onErrorsPF))))

  /**
    * If a `BadRequest` is returned its body will be decoded to a `List[String]` and the `onErrorPF` will be invoked. Allowing to recover for some errors.
    * Otherwise an error will be raised with `UnexpectedStatus`.
    * @param onErrorsPF the `PartialFunction` to apply to the BadRequest errors.
    */
  def defaultErrorHandler[A](onErrorsPF: List[String] ?=> F[A]): Response[F] => F[A] = {
    case BadRequest(response) =>
      implicit val d = Decoder[List[String]].at("errors")
      response.as[List[String]].flatMap(errors => onErrorsPF.applyOrElse(errors, raise))
    case response => F.raiseError(UnexpectedStatus(response.status))
  }

  private def raise[A](errors: List[String]): F[A] = F.raiseError(ErroredRequest(errors))
}