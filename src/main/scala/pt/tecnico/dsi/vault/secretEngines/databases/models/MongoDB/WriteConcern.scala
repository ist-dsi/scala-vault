package pt.tecnico.dsi.vault.secretEngines.databases.models.MongoDB

import scala.concurrent.duration.{Duration, FiniteDuration}
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.syntax._
import pt.tecnico.dsi.vault.decoderFiniteDuration

object WriteConcern {
  val firstEncoder: Encoder.AsObject[WriteConcern] =
    Encoder.forProduct5("w", "wmode", "wtimeout", "j", "fsync")(w => (w.minNumberOfWrites, w.writeMode, w.timeout.toMillis, w.j, w.fsync))
  val lastDecoder: Decoder[WriteConcern] =
    Decoder.forProduct5("w", "wmode", "wtimeout", "j", "fsync")(WriteConcern.apply)

  implicit val encoder: Encoder[WriteConcern] = Encoder.encodeString.contramap(_.asJson(firstEncoder).noSpaces)
  implicit val decoder: Decoder[WriteConcern] = Decoder[String].emap(decode(_)(lastDecoder).left.map(_.getMessage))
}
/**
  * This class does not correspond directly to [[https://docs.mongodb.com/manual/reference/write-concern/ MongoDB Write Concern]] but rather
  * to [[https://godoc.org/gopkg.in/mgo.v2#Safe mgo Safe]]. See [[https://www.vaultproject.io/api/secret/databases/mongodb#write_concern Vault MongoDB]].
  * @param minNumberOfWrites how many servers should confirm a write before the operation is considered successful.
  *                          When set to 0 - requests no acknowledgment of the write operation. If you also set waitForAcknowledge to true,
  *                          that prevails to request acknowledgment from the standalone mongod or the primary of a replica set.
  *                          When set to 1 - requests acknowledgment that the write operation has propagated to the standalone mongod or
  *                          the primary in a replica set. This is the default.
  *                          When set to greater than 1 - requires acknowledgment from the primary and as many additional data-bearing secondaries to meet the
  *                          specified value. For example, consider a 3-member replica set with no arbiters. Specifying 2 would require acknowledgment from
  *                          the primary and one of the secondaries. Specifying 3 would require acknowledgment from the primary and both secondaries.
  * @param writeMode When set to "majority" - requests acknowledgment that write operations have propagated to the calculated majority of the data-bearing
  *                  voting members (i.e. primary and secondaries with members[n].votes greater than 0).
  *                  When set to a `custom write concern name` - Requests acknowledgment that the write operations have propagated to tagged
  *                  members that satisfy the custom write concern defined in settings.getLastErrorModes.
  *                  This takes priority over minNumberOfWrites.
  * @param timeout specifies a time limit for the write concern. This is only applicable for minNumberOfWrites values greater than 1.
  * @param j     if set to true, servers will block until write operations have been committed to the journal. Cannot be used in combination with `fsync`.
  *              Prior to MongoDB 2.6 this option was ignored if the server was running without journaling.
  *              Starting with MongoDB 2.6 write operations will fail with an exception if this option is used when the server is running without journaling.
  * @param fsync If set to true and the server is running without journaling, blocks until the server has synced all data files to disk.
  *              If the server is running with journaling, this acts the same as the `j` option, blocking until write operations have been committed to the journal.
  *              Cannot be used in combination with `j`.
  */
case class WriteConcern(
  minNumberOfWrites: Option[Int] = Some(1),
  writeMode: Option[String] = None,
  timeout: FiniteDuration = Duration.Zero,
  j: Option[Boolean] = Some(true),
  fsync: Option[Boolean] = None,
) {
  if (timeout > Duration.Zero && !(minNumberOfWrites.exists(_ > 1) || writeMode.isDefined)) {
    throw new IllegalArgumentException("requirement failed: timeout can only be used when minNumberOfWrites is greater than 1 or writeMode is defined.")
  }
}