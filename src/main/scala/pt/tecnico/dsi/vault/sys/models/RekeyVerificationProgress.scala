package pt.tecnico.dsi.vault.sys.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object RekeyVerificationProgress {
  implicit val decoder: Decoder[RekeyVerificationProgress] = deriveDecoder(renaming.snakeCase)
}

/**
  * @param nonce the nonce for the current rekey operation.
  * @param t is the threshold required for the new shares to pass verification
  * @param n the total number of new shares that were generated
  * @param progress is how many of the new unseal keys have been provided for this verification operation
  */
case class RekeyVerificationProgress(nonce: String, t: Int, n: Int, progress: Int)

