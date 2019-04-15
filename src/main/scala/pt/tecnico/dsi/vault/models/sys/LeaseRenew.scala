package pt.tecnico.dsi.vault.models.sys

case class LeaseRenew(leaseId: String, renewable: Boolean, leaseDuration: Int)