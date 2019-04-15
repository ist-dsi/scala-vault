package pt.tecnico.dsi.vault.models

import io.circe.generic.extras.Configuration

package object sys {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}
