package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object RekeyProgress {
  implicit val decoder: Decoder[RekeyProgress] = deriveDecoder(renaming.snakeCase, false, None)
}
case class RekeyProgress(started: Boolean, nonce: String, progress: Int, required: Int, complete: Boolean,
                         encodedToken: String, encodedRootToken: String, pgpFingerprint: String,
                         otp: String, otpLength: Int,
                         verificationRequired: Boolean)