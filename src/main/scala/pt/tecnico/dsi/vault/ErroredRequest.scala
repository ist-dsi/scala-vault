package pt.tecnico.dsi.vault

import scala.util.control.NoStackTrace

final case class ErroredRequest(errors: List[String]) extends RuntimeException with NoStackTrace {
  override def getMessage: String = s"Bad Request with errors:\n\t${errors.mkString("\n\t")}"
}