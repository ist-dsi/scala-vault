package pt.tecnico.dsi.vault.models.sys

import java.time.OffsetDateTime

case class Lease(leaseId: String, issueTime: OffsetDateTime, expireTime: OffsetDateTime,
                 lastRenewalTime: Option[OffsetDateTime], renewable: Boolean, ttl: Int)