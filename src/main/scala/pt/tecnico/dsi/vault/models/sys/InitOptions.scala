package pt.tecnico.dsi.vault.models.sys

/**
  * @param secretShares Specifies the number of shares to split the master key into.
  * @param secretThreshold Specifies the number of shares required to reconstruct the master key.
  *                        This must be less than or equal secretShares.
  *                        If using Vault HSM with auto-unsealing, this value must be the same as secretShares.
  * @param pgpKeys Specifies an array of PGP public keys used to encrypt the output unseal keys. Ordering is preserved.
  *                The keys must be base64-encoded from their original binary representation.
  *                The size of this array must be the same as secretShares.
  * @param rootTokenPgpKey Specifies a PGP public key used to encrypt the initial root token.
  *                        The key must be base64-encoded from its original binary representation.
  * @param recoveryShares Specifies the number of shares to split the recovery key into.
  * @param recoveryThreshold Specifies the number of shares required to reconstruct the recovery key.
  *                          This must be less than or equal to recoveryShares.
  * @param recoveryPgpKeys Specifies an array of PGP public keys used to encrypt the output recovery keys.
  *                        Ordering is preserved. The keys must be base64-encoded from their original binary representation.
  *                        The size of this array must be the same as recoveryShares.
  * @param storedShares Specifies the number of shares that should be encrypted by the HSM and stored for auto-unsealing.
  *                     Currently must be the same as secretShares.
  */
case class InitOptions(secretShares: Int = 5, secretThreshold: Int = 3,
                       pgpKeys: Option[Array[String]] = None, rootTokenPgpKey: Option[String] = None,
                       recoveryShares: Option[Int] = None, recoveryThreshold: Option[Int] = None,
                       recoveryPgpKeys: Option[Array[String]] = None, storedShares: Option[Int] = None)