package pt.tecnico.dsi.vault.models.secretEngines.consul

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}

object Policy {
  def base64encode(string: String): String = Base64.getEncoder.encodeToString(string.getBytes(StandardCharsets.UTF_8))
  def base64decode(string: String): String = new String(Base64.getDecoder.decode(string), StandardCharsets.UTF_8)

  implicit val encoder: Encoder[Policy] = Encoder.encodeString.contramap(p => base64encode(p.rules))
  implicit val decoder: Decoder[Policy] = Decoder.decodeString.emap(p => Right(new Policy(base64decode(p))))
}
// Opaque types would be awesome here.
class Policy(val rules: String) extends AnyVal {
  override def toString = s"Policy($rules)"
}

object Role {
  // I can't understand why this is needed
  implicit val c = pt.tecnico.dsi.vault.models.config

  implicit val decoder: Decoder[Role] = deriveDecoder
  implicit val encoder: Encoder[Role] = deriveEncoder
}

case class Role(policy: Policy, tokenType: TokenType = Client, ttl: Int = 0, maxTtl: Int = 0)