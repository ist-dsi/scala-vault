package pt.tecnico.dsi.vault.secretEngines.databases.models.Elasticsearch

import scala.concurrent.duration.Duration
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.Decoder.Result
import pt.tecnico.dsi.vault.{decoderDuration, encodeDuration}
import pt.tecnico.dsi.vault.secretEngines.databases.models.BaseRole

object Role {
  val encoder: Encoder[Role] = Encoder.forProduct4("db_name", "creation_statements", "default_ttl", "max_ttl")(r =>
    (r.dbName, r.creationStatements, r.defaultTtl, r.maxTtl)
  )
  val decoder: Decoder[Role] = new Decoder[Role] {
    private def decodeTo[A: Decoder](cursor: HCursor, at: String) =
      decode[A](at).left.map(e => DecodingFailure(e.getMessage, cursor.downField(at).history))

    override def apply(c: HCursor): Result[Role] =
      for {
        dbName <- c.get[String]("db_name")
        creationStatementsJson <- decodeTo[JsonObject](c, "creation_statements")
        defaultTtl <- c.get[Duration]("default_ttl")
        maxTtl <- c.get[Duration]("max_ttl")
      } yield Role(dbName, creationStatementsJson, defaultTtl, maxTtl)
  }
  implicit val codec: Codec[Role] = Codec.from(decoder, encoder)

  def apply(dbName: String, roleDefinition: RoleDefinition, defaultTtl: Duration, maxTtl: Duration): Role =
    new Role(dbName, roleDefinition.asJsonObject, defaultTtl, maxTtl)
}

/**
  * @param dbName the name of the database connection to use for this role.
  * @param creationStatementsJson Using JSON, either defines an `elasticsearch_role_definition` or a group of pre-existing `elasticsearch_roles`.
  *                               The object specified by the `elasticsearch_role_definition` is the JSON directly passed through to the
  *                               Elasticsearch API, so you can pass through anything shown
  *                               <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.6/security-api-put-role.html">here</a>.
  *                               @see [[RoleDefinition]]
  *                               For `elasticsearch_roles`, add the names of the roles only. They must pre-exist in Elasticsearch.
  *                               Defining roles in Vault is more secure than using pre-existing roles because a privilege escalation could
  *                               be performed by editing the roles used out-of-band in Elasticsearch.
  * @param defaultTtl the TTL for the leases associated with this role. Defaults to system/engine default TTL time.
  * @param maxTtl the maximum TTL for the leases associated with this role.
  *               Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL
  *               (or, if not set, the system max TTL), but it is not allowed to be longer.
  *               @see See also [[https://www.vaultproject.io/docs/concepts/tokens.html#the-general-case The TTL General Case]].
  */
final case class Role(dbName: String, creationStatementsJson: JsonObject, defaultTtl: Duration, maxTtl: Duration) extends BaseRole {
  override val creationStatements: List[String] = List(creationStatementsJson.asJson.noSpaces)
}