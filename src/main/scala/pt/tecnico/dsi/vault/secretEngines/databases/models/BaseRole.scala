package pt.tecnico.dsi.vault.secretEngines.databases.models

import scala.concurrent.duration.Duration
import io.circe.{parser, Encoder, Decoder, DecodingFailure, HCursor}
import pt.tecnico.dsi.vault.encodeDuration

object BaseRole {
  /**
    * Using `cursor` read the field `at` as a String, then parse it as Json and decode it as `A`
    * @param cursor the cursor to use for the decoding.
    * @param at the name of field.
    * @tparam A the type to be decoded into
    */
  def decodeJsonStringDownField[A: Decoder](cursor: HCursor, at: String): Either[DecodingFailure, A] = {
    val cursorAt = cursor.downField(at)
    for {
      input <- cursorAt.as[String]
      result <- parser.decode[A](input).left.map(e => DecodingFailure(e.getMessage, cursorAt.history))
    } yield result
  }

  def encoder[T <: BaseRole]: Encoder.AsObject[T] =
    Encoder.forProduct4("db_name", "creation_statements", "default_ttl", "max_ttl")(r =>
      (r.dbName, r.creationStatements, r.defaultTtl, r.maxTtl)
    )
}
trait BaseRole {
  /** The name of the database connection to use for this role. */
  def dbName: String
  /** The database statements executed to create and configure a user. */
  def creationStatements: List[String]
  /** The TTL for the leases associated with this role. Defaults to system/engine default TTL time. */
  def defaultTtl: Duration
  /**
    * The maximum TTL for the leases associated with this role.
    * Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL
    * (or, if not set, the system max TTL), but it is not allowed to be longer.
    * @see See also [[https://www.vaultproject.io/docs/concepts/tokens.html#the-general-case The TTL General Case]]
    **/
  def maxTtl: Duration
}
