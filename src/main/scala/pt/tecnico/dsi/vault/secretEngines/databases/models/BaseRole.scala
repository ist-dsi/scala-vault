package pt.tecnico.dsi.vault.secretEngines.databases.models

import scala.concurrent.duration.Duration

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
