package pt.tecnico.dsi.vault.models.sys

case class UnsealOptions(key: String, reset: Option[Boolean] = None, migrate: Option[Boolean] = None)