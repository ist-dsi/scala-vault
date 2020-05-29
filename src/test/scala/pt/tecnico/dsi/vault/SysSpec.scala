package pt.tecnico.dsi.vault

import scala.concurrent.duration.DurationInt

class SysSpec extends Utils {
  "The init endpoint" should {
    "return the initialization status" in idempotently {
      client.sys.init.initialized().map(_ shouldBe true)
    }
    // We cannot test initialize because the vault docker comes already initialized.
  }

  "The health endpoint" should {
    "return the health status" in idempotently {
      client.sys.health.status().map { status =>
        status.`sealed` shouldBe false
        status.initialized shouldBe true
        status.standby shouldBe false
      }
    }
  }

  "The leader endpoint" should {
    "return the leader status" in idempotently {
      client.sys.leader.status().map { status =>
        status.haEnabled shouldBe false
        status.performanceStandby shouldBe false
      }
    }
    "be able to step down" in idempotently {
      client.sys.leader.stepDown().map(_ shouldBe ())
    }
  }

  "The seal endpoint" should {
    "return the seal status" in idempotently {
      client.sys.seal.status().map { status =>
        status.`sealed` shouldBe false
        status.secretShares shouldBe 1
        status.secretThreshold shouldBe 1
      }
    }
    /*"seal vault" in {
      client.sys.seal.seal().value(_ shouldBe ())
    }*/

    "unseal vault" in {
      client.sys.seal.unseal(unsealKey).value { status =>
        status.`sealed` shouldBe false
        status.secretShares shouldBe 1
        status.secretThreshold shouldBe 1
      }
    }
  }

  "The generate root endpoint" should {
    "start a generation" in {
      client.sys.generateRoot.start().value { status =>
        status.complete shouldBe false
        status.progress shouldBe 0
        status.required shouldBe 1
      }
    }

    "return the root generation progress" in {
      client.sys.generateRoot.progress().value { progress =>
        progress.complete shouldBe false
        progress.progress shouldBe 0
        progress.required shouldBe 1
      }
    }

    "cancel the root generation progress" in idempotently {
      client.sys.generateRoot.cancel().map(_ shouldBe ())
    }

    "generate a root token" in {
      val io = for {
        start <- client.sys.generateRoot.start()
        progress <- client.sys.generateRoot.put(unsealKey, start.nonce)
      } yield {
        progress.complete shouldBe true
        progress.progress shouldBe 1
        progress.required shouldBe 1
        progress.started shouldBe true
        progress.nonce shouldBe start.nonce
        progress.decode() shouldBe empty // The otp only exists for the progress returned from start
        val token = progress.decode(start.otp)
        logger.info(s"RootGeneration produced ${token}")
        token should not be empty
      }
      io.unsafeToFuture()
    }
  }

  "The leases endpoint" should {
    import pt.tecnico.dsi.vault.authMethods.token.models.CreateOptions
    // Unfortunately we first need to create a token in order to have leases
    client.authMethods.token.create(CreateOptions(ttl = 5.minute, explicitMaxTtl = 20.minutes)).unsafeRunSync()
    val prefix = "auth/token/create"

    "list leases" in idempotently {
      client.sys.leases.list(prefix).map(_.length should be >= 1)
    }

    "read info about a lease" in idempotently {
      for {
        leases <- client.sys.leases.list(prefix)
        lease <- client.sys.leases(s"$prefix/${leases.last}")
      } yield lease.renewable shouldBe true
    }

    /*"renew a lease" in idempotently {
      for {
        leases <- client.sys.leases.list(prefix)
        // We are getting a permission denied. https://github.com/hashicorp/vault/issues/7297
        renew <- client.sys.leases.renew(s"$prefix/${leases.last}")
        lease <- client.sys.leases(s"$prefix/${leases.last}")
      } yield {
        renew.renewable shouldBe true
        assert(renew.leaseDuration >= 1.minute)
        lease.renewable shouldBe true
      }
    }*/

    /*
    "revoke a lease" in {
      val io = for {
        leases <- client.sys.leases.list(prefix)
        revoke <- client.sys.leases.revoke(s"$prefix/${leases.last}")
        //lease <- client.sys.leases.get(s"$prefix/${leases.last}")
      } yield {
        revoke shouldBe ()
        //lease shouldBe empty
      }
      io.unsafeToFuture()
    }
    */
    // TODO: revokeForce and revokePrefix
  }

  "The policy endpoint" should {
    import pt.tecnico.dsi.vault.sys.models.Policy

    "create a policy" in idempotently {
      client.sys.policy.create("test", Policy("""path "*" { capabilities = ["read"] }""")).map(_ shouldBe ())
    }
    "edit a policy" in {
      for {
        _ <- client.sys.policy.create("test2", Policy("""path "*" { capabilities = ["read", "delete"] }""")).value(_ shouldBe ())
        // This is also testing `get`
        _ <- client.sys.policy("test2").value(_.rules should include ("delete"))
        result <- client.sys.policy("test2").idempotently(_.rules should not contain "delete")
      } yield result
    }
    "list policies" in idempotently {
      //TODO: this test should stand on its own
      client.sys.policy.list().map(_ should contain allOf ("test", "test2"))
    }
    "delete a policy" in idempotently {
      for {
        first <- client.sys.policy.delete("test2")
        list <- client.sys.policy.list()
      } yield {
        first shouldBe ()
        list should not contain ("teste2")
      }
    }
  }

  "The auth endpoint" should {
    import pt.tecnico.dsi.vault.sys.models.AuthMethod
    import pt.tecnico.dsi.vault.sys.models.AuthMethod.TuneOptions
    def createAuthMethod(`type`: String): AuthMethod = AuthMethod(`type`, "description", TuneOptions(defaultLeaseTtl = 1.hour, maxLeaseTtl = 30.days))

    "mount an authentication method" in idempotently {
      client.sys.auth.enable("test", createAuthMethod("approle")).map(_ shouldBe ())
    }
    "edit an authentication method" in {
      val method = AuthMethod("approle", "description", TuneOptions(defaultLeaseTtl = 1.day))
      for {
        _ <- client.sys.auth.enable("test2", method).value(_ shouldBe ())
        result <- client.sys.auth("test2").idempotently(_.defaultLeaseTtl shouldBe 1.day)
      } yield result
    }
    "list authentication methods" in idempotently {
      client.sys.auth.list().map { authMethods =>
        authMethods should contain key "test/"
        authMethods should contain key "token/"
        authMethods.values.map(_.config) should contain(createAuthMethod("dummy").config)
      }
    }
    "disable an authentication method" in idempotently {
      for {
        first <- client.sys.auth.disable("test2")
        list <- client.sys.auth.list()
      } yield {
        first shouldBe ()
        list should not contain key ("teste2/")
      }
    }
  }

  "The mounts endpoint" should {
    import pt.tecnico.dsi.vault.sys.models.SecretEngine
    import pt.tecnico.dsi.vault.sys.models.SecretEngine.TuneOptions
    def createSecretEngine(`type`: String): SecretEngine = SecretEngine(`type`, "description", TuneOptions(defaultLeaseTtl = 1.hour, maxLeaseTtl = 30.days))

    "mount a secret engine" in idempotently {
      client.sys.mounts.enable("test", createSecretEngine("consul")).map(_ shouldBe ())
    }
    "edit a secret engine" in {
      val engine = SecretEngine("consul", "", TuneOptions(defaultLeaseTtl = 1.day))
      for {
        _ <- client.sys.mounts.enable("test2", engine).value(_ shouldBe ())
        result <- client.sys.mounts("test2").idempotently(_.defaultLeaseTtl shouldBe 1.day)
      } yield result
    }
    "list authentication methods" in idempotently {
      client.sys.mounts.list().map { secretEngines =>
        secretEngines should contain key "test/"
        secretEngines should contain key "test2/"
        secretEngines.values.map(_.config) should contain(createSecretEngine("dummy").config)
      }
    }
    "disable a secret engine" in idempotently {
      for {
        first <- client.sys.mounts.disable("test2")
        list <- client.sys.mounts.list()
      } yield {
        first shouldBe ()
        list should not contain key ("teste2/")
      }
    }
    "remount a secret engine" in {
      // The second remount is just so we can re-run the tests
      val io = for {
        _ <- client.sys.mounts.enable("secret", createSecretEngine("kv"))
        firstRemount <- client.sys.mounts.remount("secret", "new-secret")
        read <- client.sys.mounts.list()
        secondRemount <- client.sys.mounts.remount("new-secret", "secret")
      } yield {
        firstRemount shouldBe ()
        read.keys should contain ("new-secret/")
        secondRemount shouldBe ()
      }
      io.unsafeToFuture()
    }
  }
}
