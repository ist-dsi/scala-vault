package pt.tecnico.dsi.vault.models.authMethods

import io.circe.generic.extras.Configuration

package object token {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}
