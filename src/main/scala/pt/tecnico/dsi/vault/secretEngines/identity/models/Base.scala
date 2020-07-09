package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime

// Find a better name for this
trait Base {
  def id: String
  def name: String
  def creationTime: OffsetDateTime
  def lastUpdateTime: OffsetDateTime
  def policies: List[String]
  def namespaceId: String
  def metadata: Map[String, String]
}
