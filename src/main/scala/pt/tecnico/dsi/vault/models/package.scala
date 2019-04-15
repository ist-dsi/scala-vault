package pt.tecnico.dsi.vault

import io.circe.generic.extras.Configuration

package object models {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}
