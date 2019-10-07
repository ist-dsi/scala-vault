package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.Decoder
import io.circe.derivation._

object CSR {
  implicit val decoder: Decoder[CSR] = deriveDecoder(renaming.snakeCase, false, None)
}
case class CSR(csr: String, privateKey: Option[String] = None, privateKeyType: Option[KeySettings.Type] = None)

/*object Test {
  sealed trait Type { type Out }
  case object Exported extends Type { type Out = CSRAndKey }
  case object Internal extends Type { type Out = CSR }

  case class CSR(csr: String)
  case class CSRAndKey(csr: String, private_key: String, private_key_type: String)

  val csrJson = """{ "csr": "a csr value" }"""
  val csrAndPrivateJson = """{ "csr": "a csr value", "private_key": "key", "private_key_type": "rsa" }"""

  def generateIntermediate(result: String, `type`: Type): `type`.Out = {
    io.circe.parser.decode[`type`.Out](result).toOption.get
  }
}*/