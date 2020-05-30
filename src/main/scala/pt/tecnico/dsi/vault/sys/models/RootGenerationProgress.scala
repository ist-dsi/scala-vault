package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object RootGenerationProgress {
  implicit val decoder: Decoder[RootGenerationProgress] = deriveDecoder(renaming.snakeCase)

  def decode(otp: String, encodedRootToken: String): String = {
    import java.util.Base64
    val zippedBytes = otp.getBytes zip Base64.getDecoder.decode(encodedRootToken)
    new String(zippedBytes.map { case (a, b) => (a ^ b).toByte })
  }
}

/**
  * @param started whether a root generation attempt has been started.
  * @param nonce the nonce for the current attempt.
  * @param progress how many unseal keys have been provided for this generation attempt.
  * @param required how many unseal keys must be provided to complete the generation attempt.
  * @param complete whether the attempt is complete.
  * @param encodedToken the encoded token. The token will either be encrypted using PGP or XOR'd using the OTP.
  * @param encodedRootToken the encoded root token. The token will either be encrypted using PGP or XOR'd using the OTP.
  * @param pgpFingerprint the PGP fingerprint used to encrypt the final root token.
  *                       This will be an empty string unless a PGP key is being used to encrypt the final root token.
  * @param otp the one-time-password (OTP) being used to encode the final root token.
  *            The OTP is a base64 string, with length of `otpLength`.
  *            The raw bytes (char codes) of the token will be XOR'd with this value before being returned as a response
  *            to the final unseal key, encoded as base64.
  *            This field will only be returned once, on the response to the start request.
  * @param otpLength the size of the OTP.
  */
case class RootGenerationProgress(started: Boolean, nonce: String, progress: Int, required: Int, complete: Boolean,
                                  encodedToken: String, encodedRootToken: String, pgpFingerprint: String, otp: String, otpLength: Int) {
  /** Whether this root generation is in progress. */
  def inProgress: Boolean = required > 0

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

