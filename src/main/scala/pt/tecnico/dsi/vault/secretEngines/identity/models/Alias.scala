package pt.tecnico.dsi.vault.secretEngines.identity.models

import java.time.OffsetDateTime

trait Alias {
  def id: String
  def canonicalId: String
  def name: String
  def creationTime: OffsetDateTime
  def lastUpdateTime: OffsetDateTime
  def mountAccessor: String
  def mountPath: String
  def mountType: String
  def metadata: Map[String, String]
}