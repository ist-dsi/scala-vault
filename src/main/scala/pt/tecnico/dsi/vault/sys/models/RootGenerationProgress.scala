package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object RootGenerationProgress {
  implicit val decoder: Decoder[RootGenerationProgress] = deriveDecoder(renaming.snakeCase, false, None)

  def decode(otp: String, encodedRootToken: String): String = {
    import java.util.Base64
    val zippedBytes = otp.getBytes zip Base64.getDecoder.decode(encodedRootToken)
    new String(zippedBytes.map { case (a, b) => (a ^ b).toByte })
  }
}
case class RootGenerationProgress(started: Boolean, nonce: String, progress: Int, required: Int, complete: Boolean,
                                  encodedToken: String, encodedRootToken: String, pgpFingerprint: String, otp: String, otpLength: Int) {
  /**
    * Decode the `encodedRootToken` of this `RootGenerationProgress` using the provided `otp`.
    * If a PGP key was passed at the start of the root generation attempt, this method will do nothing.
    * @return the decoded root token. If `encodedRootToken` or `otp` are empty this method will return a `None`.
    */
  def decode(otp: String = this.otp): Option[String] = {
    for {
      otp <- Option(otp) if otp.nonEmpty
      encodedRootToken <- Option(encodedRootToken) if encodedRootToken.nonEmpty
    } yield RootGenerationProgress.decode(otp, encodedRootToken)
  }
}

