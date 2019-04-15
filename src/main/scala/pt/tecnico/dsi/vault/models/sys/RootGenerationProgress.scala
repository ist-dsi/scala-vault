package pt.tecnico.dsi.vault.models.sys

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RootGenerationProgress(started: Boolean, nonce: String, progress: Int, required: Int, complete: Boolean,
                                  encodedToken: String, encodedRootToken: String, pgpFingerprint: String, otp: String, otpLength: Int)