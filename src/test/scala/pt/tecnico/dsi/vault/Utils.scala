package pt.tecnico.dsi.vault

import cats.effect.{ContextShift, IO, Timer}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.syntax.literals._
import org.log4s._
import org.scalatest.exceptions.TestFailedException
import org.scalatest._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import scala.sys.process._

abstract class Utils extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  val logger: Logger = getLogger
  implicit override def executionContext: ExecutionContextExecutor = ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  val (httpClient, finalizer) = BlazeClientBuilder[IO](global)
    .withResponseHeaderTimeout(10.seconds)
    .withCheckEndpointAuthentication(false)
    .resource.allocated.unsafeRunSync()
  override protected def afterAll(): Unit = finalizer.unsafeRunSync()

  private implicit val c = httpClient

  val ignoreStdErr = ProcessLogger(_ => ())
  val UnsealKeyRegex = """(?<=Unseal Key: )([^\n]+)""".r.unanchored
  val RootTokenRegex = """(?<=Root Token: )([^\n]+)""".r.unanchored
  val dockerLogLines = "docker logs dev-vault".lazyLines(ignoreStdErr)
  val unsealKey = dockerLogLines.collect{ case UnsealKeyRegex(key) => key }.last
  val rootToken = dockerLogLines.collect{ case RootTokenRegex(token) => token }.last
  logger.info(s"Unseal Key: $unsealKey\nRoot Token: $rootToken")
  // By default the vault container listens in 0.0.0.0:8200
  val client = new VaultClient[IO](uri"http://localhost:8200", rootToken)

  implicit class RichIO[T](io: IO[T]) {
    def value(test: T => Assertion): Future[Assertion] = io.map(test).unsafeToFuture()
    def valueShouldBe(v: T): Future[Assertion] = value(_ shouldBe v)

    def idempotently(test: T => Assertion, repetitions: Int = 3): Future[Assertion] = {
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")

      io.unsafeToFuture().flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        Future.traverse(2 to repetitions) { _ =>
          io.unsafeToFuture()
        } map { results =>
          // And now we want to catch the exception because if `test` fails here it means it is not idempotent.
          try {
            results.foreach(test)
            succeed
          } catch {
            case e: TestFailedException =>
              val numberOfDigits = Math.floor(Math.log10(repetitions.toDouble)).toInt + 1
              val resultsString = (firstResult +: results).zipWithIndex
                .map { case (result, i) =>
                  s" %${numberOfDigits}d: %s".format(i + 1, result)
                }.mkString("\n")
              throw e.modifyMessage(_.map(message => s"""Operation is not idempotent. Results:
                                                        |$resultsString
                                                        |$message""".stripMargin))
          }
        }
      }
    }
    def valueShouldIdempotentlyBe(value: T): Future[Assertion] = idempotently(_ shouldBe value)
  }

  private def ordinalSuffix(number: Int): String = {
    number % 100 match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
  }

  def idempotently(test: IO[Assertion], repetitions: Int = 3): Future[Assertion] = {
    require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")

    // If the first run fails we do not want to mask its exception, because failing in the first attempt means
    // whatever is being tested in `test` is not implemented correctly.
    test.unsafeToFuture().flatMap { _ =>
      // For the subsequent iterations we mask TestFailed with "Operation is not idempotent"
      Future.traverse(2 to repetitions) { repetition =>
        test.unsafeToFuture().transform(identity, {
          case e: TestFailedException =>
            val text = s"$repetition${ordinalSuffix(repetition)}"
            e.modifyMessage(_.map(m => s"Operation is not idempotent. On $text repetition got:\n$m"))
          case e => e
        })
      } map(_ should contain only (Succeeded)) // Scalatest flatten :P
    }
  }
}
