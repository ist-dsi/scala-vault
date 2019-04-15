package pt.tecnico.dsi.vault

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import pt.tecnico.dsi.vault.models._

//TODO: Some operations don't require a token. So the fact that we are requiring one might be misleading
class VaultClient[F[_]: Sync](val baseUri: Uri, val token: String)(implicit client: Client[F]) {
  val tokenHeader: Header = Header("X-Vault-Token", token)

  private[this] val dsl = new Http4sDsl[F] with Http4sClientDsl[F]{}
  import dsl._

  // Have you ever heard about the LIST HTTP method, neither have I, as it does not exist </faceplam>
  private val LIST = Method.fromString("LIST").right.get

  private val uri = baseUri / "v1"

  object sys {
    private val sysUri = uri / "sys"
    import pt.tecnico.dsi.vault.models.sys._

    object init {
      private val initUri = sysUri / "init"

      /** @return the initialization status of Vault. */
      def initialized(): F[Boolean] = {
        case class Status(initialized: Boolean)
        for {
          response <- client.expect[Status](GET(initUri))
        } yield response.initialized
      }

      /**
        * This endpoint initializes Vault. The Vault must not have been previously initialized.
        * The recovery options, as well as the stored shares option, are only available when using Vault HSM.
        *
        * @param options The options to use when initializing Vault.
        * @return The result of initializing Vault.
        */
      def initialize(options: InitOptions): F[InitResult] = client.expect(PUT(options, initUri))
    }

    object health {
      private val healthUri = sysUri / "health"

      /** @return the health status of Vault. This matches the semantics of a Consul HTTP health check and provides a
        *         simple way to monitor the health of a Vault instance.
        */
      def status(): F[HealthStatus] = client.expect(GET(healthUri))
    }

    object leader {
      private val leaderUri = sysUri / "leader"

      /** @return the high availability status and current leader instance of Vault. */
      def status(): F[LeaderStatus] = client.expect(GET(leaderUri))

      /**
        * Forces the node to give up active status. If the node does not have active status, this does nothing.
        * Note that the node will sleep for ten seconds before attempting to grab the active lock again, but if no
        * standby nodes grab the active lock in the interim, the same node may become the active node again.
        * Requires a token with `root` policy or `sudo` capability on the path.
        */
      def stepDown(): F[Unit] = client.expectUnit(GET(sysUri / "step-down", tokenHeader))
    }

    object seal {
      private val sealUri = sysUri / "seal"

      /** @return the seal status of the Vault. */
      def status(): F[SealStatus] = client.expect(GET(sysUri / "seal-status"))

      /**
        * Seals the Vault. In HA mode, only an active node can be sealed. Standby nodes should be restarted to get the
        * same effect. Requires a token with `root` policy or `sudo` capability on the path.
        */
      def seal(): F[Unit] = client.expectUnit(GET(sealUri))

      /**
        * This endpoint is used to enter a single master key share to progress the unsealing of the Vault.
        * If the threshold number of master key shares is reached, Vault will attempt to unseal the Vault.
        * Otherwise, this API must be called multiple times until that threshold is met.
        *
        * @param key the unseal key to use.
        * @return the current seal status of Vault.
        */
      def unseal(key: String): F[SealStatus] = unseal(UnsealOptions(key))

      /**
        * This endpoint is used to enter a single master key share to progress the unsealing of the Vault.
        * If the threshold number of master key shares is reached, Vault will attempt to unseal the Vault.
        * Otherwise, this API must be called multiple times until that threshold is met.
        *
        * Either the `key` or `reset` parameter must be provided; if both are provided, `reset` takes precedence.
        *
        * @param options the options to use for the unseal.
        */
      def unseal(options: UnsealOptions): F[SealStatus] = client.expect(PUT(options, sysUri / "unseal", tokenHeader))
    }

    object generateRoot {
      private val generateRootUri = sysUri / "generate-root"

      /** @return the configuration and progress of the current root generation attempt. */
      def progress(): F[RootGenerationProgress] = client.expect(GET(generateRootUri / "attempt"))

      /**
        * Start a new root generation attempt. Only a single root generation attempt can take place at a time.
        *
        * @param pgpKey Specifies a base64-encoded PGP public key. The raw bytes of the token will be encrypted with
        *               this value before being returned to the final unseal key provider.
        * @return the configuration and progress of the current root generation attempt.
        */
      def start(pgpKey: Option[String] = None): F[RootGenerationProgress] =
        client.expect(PUT(Map("pgp_key" -> pgpKey).asJson, generateRootUri / "attempt"))

      /**
        * Cancels any in-progress root generation attempt. This clears any progress made.
        * This must be called to change the OTP or PGP key being used.
        */
      def cancel(): F[Unit] = client.expectUnit(DELETE(generateRootUri / "attempt"))

      /**
        * This endpoint is used to enter a single master key share to progress the root generation attempt.
        * If the threshold number of master key shares is reached, Vault will complete the root generation and issue
        * the new token. Otherwise, this API must be called multiple times until that threshold is met.
        * The attempt nonce must be provided with each call.
        * @param keyShare Specifies a single master key share.
        * @param nonce Specifies the nonce of the attempt.
        */
      def put(keyShare: String, nonce: String): F[RootGenerationProgress] =
        client.expect(PUT(Map("key" -> keyShare, "nonce" -> nonce).asJson, generateRootUri / "update"))

      /**
        * Decode the encoded token returned by the root generation attempt. The OTP used during the process
        * is necessary. If a PGP key was passed at the start of the root generation attempt, this method will do nothing.
        * @param otp the OTP to use in the decoding process.
        * @param encodedToken the encoded token returned by the root generation attempt.
        * @return the decoded root tooken.
        */
      def decode(otp: String, encodedToken: String): String = {
        import java.util.Base64
        val zippedBytes = otp.getBytes zip Base64.getDecoder.decode(encodedToken)
        new String(zippedBytes.map { case (a, b) => (a ^ b).toByte })
      }

      /**
        * Decodes a root token from a `RootGenerationProgress`.
        * @param progress the `RootGenerationProgress` from which to decode the root token.
        * @return the decoded root token if both `otp` and `encodedRootToken` are present in `progress`.
        */
      def decode(progress: RootGenerationProgress): Option[String] =
        for {
          otp <- Option(progress.otp) if otp.nonEmpty
          encodedRootToken <- Option(progress.encodedRootToken) if encodedRootToken.nonEmpty
        } yield decode(otp, encodedRootToken)
    }

    object leases {
      private val leasesUri = sysUri / "leases"

      /**
        * This endpoint returns a list of lease ids. This endpoint requires 'sudo' capability.
        *
        * @param prefix the prefix for which to list leases.
        */
      def list(prefix: String): F[List[String]] = {
        for {
          request <- GET(leasesUri.withPath(leasesUri.path + "/lookup/" + prefix), tokenHeader)
          response <- client.expect[Context[Keys]](request)
        } yield response.data.keys
      }

      def apply(name: String): F[Lease] = get(name).map(_.get)
      /**
        * @param id the id of the lease.
        * @return the metadata associated with the lease with `id`. If no lease with that `id` exists a None will be returned.
        */
      def get(id: String): F[Option[Lease]] =
        for {
          request <- PUT(Map("lease_id" -> id).asJson, leasesUri / "lookup", tokenHeader) // Facepalm
          response <- client.expectOption[Lease](request)
        } yield response

      /**
        * Renews a lease, requesting to extend the lease.
        *
        * @param id Specifies the ID of the lease to extend.
        * @param increment Specifies the requested amount of time (in seconds) to extend the lease.
        * @return
        */
      def renew(id: String, increment: Int = 0): F[LeaseRenew] = {
        case class Renew(leaseId: String, increment: Int)
        client.expect(PUT(Renew(id, increment), leasesUri / "renew", tokenHeader))
      }

      /**
        * Revokes a lease immediately.
        * @param id Specifies the ID of the lease to revoke.
        */
      def revoke(id: String): F[Unit] =
        client.expectUnit(PUT(Map("lease_id" -> id).asJson, leasesUri / "revoke", tokenHeader))

      /**
        * This endpoint revokes all secrets or tokens generated under a given prefix immediately. Unlike [[revokePrefix]],
        * this ignores backend errors encountered during revocation. This is potentially very dangerous and should only
        * be used in specific emergency situations where errors in the backend or the connected backend service prevent
        * normal revocation.
        *
        * By ignoring these errors, Vault abdicates responsibility for ensuring that the issued credentials or secrets
        * are properly revoked and/or cleaned up. Access to this endpoint should be tightly controlled.
        *
        * This endpoint requires 'sudo' capability.
        *
        * @param prefix the prefix to revoke.
        */
      def revokeForce(prefix: String): F[Unit] =
        client.expectUnit(PUT(leasesUri / "revoke-force" / prefix, tokenHeader))

      /**
        * This endpoint revokes all secrets (via a lease ID prefix) or tokens (via the tokens' path property) generated
        * under a given prefix immediately. This requires `sudo` capability and access to it should be tightly
        * controlled as it can be used to revoke very large numbers of secrets/tokens at once.
        *
        * @param prefix the prefix to revoke.
        */
      def revokePrefix(prefix: String): F[Unit] =
        client.expectUnit(PUT(leasesUri / "revoke-prefix" / prefix, tokenHeader))
    }

    object policy {
      private val policyUri = sysUri / "policy"

      /** @return all configured policies. */
      def list(): F[List[String]] = {
        case class Policies(policies: List[String])
        for {
          request <- GET(policyUri, tokenHeader)
          response <- client.expect[Policies](request)
        } yield response.policies
      }

      def apply(name: String): F[Policy] = get(name).map(_.get)
      /** @return the policy associated with `name`. */
      def get(name: String): F[Option[Policy]] =
        for {
          request <- GET(policyUri / name, tokenHeader)
          response <- client.expectOption[Policy](request)
        } yield response

      def +=(policy: Policy): F[Unit] = create(policy)
      /** Adds a new or updates an existing policy. Once a policy is updated, it takes effect immediately to all associated users. */
      def create(policy: Policy): F[Unit] = client.expectUnit(PUT(policy, policyUri / policy.name, tokenHeader))

      def -=(policy: Policy): F[Unit] = delete(policy)
      def delete(policy: Policy): F[Unit] = delete(policy.name)
      def -=(name: String): F[Unit] = delete(name)
      /**
        * Deletes the policy with the given `name`. This will immediately affect all users associated with this policy.
        * @param name the name of the policy to delete.
        */
      def delete(name: String): F[Unit] = client.expectUnit(DELETE(policyUri / name, tokenHeader))
    }
  }

  object secretEngines {
    class KeyValueV1SecretEngine(uri: Uri) {
      def list(path: String): F[List[String]] = {
        val request = Request[F](LIST, uri / path, headers = Headers(tokenHeader))
        for {
          response <- client.expect[Context[Keys]](request)
        } yield response.data.keys
      }

      def apply(path: String): F[Map[String, String]] = read(path)
      def get(path: String): F[Map[String, String]] = read(path)
      def read(path: String): F[Map[String, String]] =
        for {
          request <- GET(uri / path, tokenHeader)
          response <- client.expect[Context[Map[String, String]]](request)
        } yield response.data

      def +=(path: String, data: Map[String, String]): F[Unit] = create(path, data)
      def create(path: String, data: Map[String, String]): F[Unit] = client.expectUnit(PUT(data, uri / path, tokenHeader))

      def -=(path: String): F[Unit] = delete(path)
      def delete(path: String): F[Unit] = client.expectUnit(DELETE(uri / path, tokenHeader))
    }
    def kvV1(at: String): KeyValueV1SecretEngine = new KeyValueV1SecretEngine(uri.withPath(uri.path + "/" + at))

    class ConsulSecretEngine(uri: Uri) {
      def configureAccess(uri: Uri, token: String): F[Unit] = {
        import org.http4s.Uri.Scheme
        val data = Map(
          "address" -> s"${uri.host}:${uri.port}",
          "scheme" -> uri.scheme.getOrElse(Scheme.http).value,
          "token" -> token
        )
        client.expectUnit(POST(data.asJson, uri / "config" / "access", tokenHeader))
      }

      def generateCredential(role: String): F[String] = {
        case class CredentialResponse(token: String)
        for {
          request <- GET(uri / "creds" / role, tokenHeader)
          response <- client.expect[Context[CredentialResponse]](request)
        } yield response.data.token
      }

      object roles {
        import pt.tecnico.dsi.vault.models.secretEngines.consul._

        private val rolesUri = uri / "roles"

        def list(): F[List[String]] = {
          val request = Request[F](LIST, rolesUri, headers = Headers(tokenHeader))
          for {
            response <- client.expect[Context[Keys]](request)
          } yield response.data.keys
        }

        def apply(name: String): F[Role] = get(name).map(_.get)
        def get(name: String): F[Option[Role]] =
          for {
            request <- GET(rolesUri / name, tokenHeader)
            response <- client.expectOption[Context[Role]](request)
          } yield response.map(_.data)

        //def +=(name: String, role: Role): F[Unit] = create(name, role)
        def create(name: String, role: Role): F[Unit] = {
          println(rolesUri / name)
          client.expectUnit(POST(role.asJson, rolesUri / name, tokenHeader))
        }

        def -=(name: String): F[Unit] = delete(name)
        def delete(name: String): F[Unit] = client.expectUnit(DELETE(rolesUri / name, tokenHeader))
      }
    }
    def consul(at: String): ConsulSecretEngine = new ConsulSecretEngine(uri.withPath(uri.path + "/" + at))
  }

  object authMethods {
    object token {
      // The Token auth method is always mounted at this location. And cannot be changed.
      private val tokenUri = uri / "auth" / "token"

      import pt.tecnico.dsi.vault.models.authMethods.token._

      /** @return a list with all token accessor. This requires sudo capability, and access to it should be tightly
        *         controlled as the accessors can be used to revoke very large numbers of tokens and their associated
        *         leases at once. */
      def accessors(): F[List[String]] = {
        val request = Request[F](LIST, tokenUri / "accessors", headers = Headers(tokenHeader))
        for {
          response <- client.expect[Context[Keys]](request)
        } yield response.data.keys
      }

      /**
        * Creates a new token. Certain options are only available when called by a root token.
        * Orphan tokens require a root token to be created. However invoking `createOrphan` allows one to be created
        * without a root token.
        * If a role is used, the token will be created against the specified role name; this may override options set during this call.
        * @param options the options to use while creating the token.
        * @return the created token and its properties.
        */
      def create(options: TokenCreateOptions): F[Auth] =
        for {
          request <- POST(options, tokenUri / "create", tokenHeader)
          response <- client.expect[Context[Option[Unit]]](request)
        } yield response.auth.get
      /**
        * Creates a new token. Certain options are only available when called by a root token.
        * If a role is used, the token will be created against the specified role name; this may override options set during this call.
        * @param options the options to use while creating the token.
        * @return the created token and its properties
        */
      def createOrphan(options: TokenCreateOptions): F[Auth] =
        for {
          request <- POST(options.copy(noParent = true), tokenUri / "create", tokenHeader)
          response <- client.expect[Context[Option[Unit]]](request)
        } yield response.auth.get

      /** @param token the token for which to retrieve information.
        * @return returns information about the `token`.
        */
      def lookup(token: String): F[Token] =
        for {
          request <- POST(Map("token" -> token).asJson, tokenUri / "lookup", tokenHeader)
          response <- client.expect[Context[Token]](request)
        } yield response.data

      /** @return returns information about the current client token.
        */
      def lookupSelf(): F[Token] =
        for {
          request <- POST(tokenUri / "lookup-self", tokenHeader)
          response <- client.expect[Context[Token]](request)
        } yield response.data

      /** @param accessor the token accessor for which to retrieve the information.
        * @return returns information about the client token from the `accessor`.
        */
      def lookupAccessor(accessor: String): F[Token] =
        for {
          request <- POST(Map("accessor" -> accessor).asJson, tokenUri / "lookup", tokenHeader)
          response <- client.expect[Context[Token]](request)
        } yield response.data

      /**
        * Renews a lease associated with a token. This is used to prevent the expiration of a token, and the automatic
        * revocation of it. Token renewal is possible only if there is a lease associated with it.
        * @param token the token to renew.
        * @param increment An optional requested lease increment can be provided. This increment may be ignored.
        * @return the renewed token information.
        */
      def renew(token: String, increment: Option[Int] = None): F[Auth] = {
        case class Renew(token: String, increment: Option[Int])
        for {
          request <- POST(Renew(token, increment), tokenUri / "renew", tokenHeader)
          response <- client.expect[Context[Option[Unit]]](request)
        } yield response.auth.get
      }

      /**
        * Renews a lease associated with the calling token. This is used to prevent the expiration of a token, and the
        * automatic revocation of it. Token renewal is possible only if there is a lease associated with it.
        * @param increment An optional requested lease increment can be provided. This increment may be ignored.
        * @return the renewed token information.
        */
      def renewSelf(increment: Option[Int] = None): F[Auth] =
        for {
          request <- POST(Map("increment" -> increment).asJson, tokenUri / "renew-self", tokenHeader)
          response <- client.expect[Context[Option[Unit]]](request)
        } yield response.auth.get

      /**
        * Revokes a token and all child tokens. When the token is revoked, all dynamic secrets generated with it are also revoked.
        * @param token the token to revoke.
        */
      def revoke(token: String): F[Unit] =
        client.expectUnit(POST(Map("token" -> token).asJson, tokenUri / "revoke", tokenHeader))

      /** Revokes the token used to call it and all child tokens. When the token is revoked,
        * all dynamic secrets generated with it are also revoked. */
      def revokeSelf(): F[Unit] =
        client.expectUnit(POST(tokenUri / "revoke-self", tokenHeader))

      /**
        * Revoke the token associated with the accessor and all the child tokens. This is meant for purposes where there
        * is no access to token ID but there is need to revoke a token and its children.
        * @param accessor Accessor of the token.
        */
      def revokeAccessor(accessor: String): F[Unit] =
        client.expectUnit(POST(Map("accessor" -> accessor).asJson, tokenUri / "revoke-accessor", tokenHeader))

      /**
        * Revokes a token but not its child tokens. When the token is revoked, all secrets generated with it are also revoked.
        * All child tokens are orphaned, but can be revoked sub-sequently using /auth/token/revoke/. This is a root-protected endpoint.
        * @param token Token to revoke.
        */
      def revokeTokenAndOrphanChildren(token: String): F[Unit] =
        client.expectUnit(POST(Map("token" -> token).asJson, tokenUri / "revoke-orphan", tokenHeader))

      object roles {
        private val rolesUri = tokenUri / "roles"

        /**
          * @return a list with the names of the available token roles. To get the `TokenRoles` you can do:
          * {{{
          *   import cats.implicits._
          *   val roles = client.authMethods.token.roles
          *   roles.list().flatMap(_.map(roles.apply).sequence)
          * }}}
          */
        def list(): F[List[String]] = {
          val request = Request[F](LIST, rolesUri, headers = Headers(tokenHeader))
          for {
            response <- client.expect[Context[Keys]](request)
          } yield response.data.keys
        }

        /**
          * Fetches the named role configuration.
          * @param name the name of the role to fetch
          * @return
          */
        def apply(name: String): F[TokenRole] = get(name).map(_.get)

        /**
          * Fetches the named role configuration.
          * @param name the name of the role to fetch
          * @return if a role named `name` exists a `Some` will be returned with its configuration. `None` otherwise.
          */
        def get(name: String): F[Option[TokenRole]] =
          for {
            request <- GET(rolesUri / name, tokenHeader)
            response <- client.expectOption[Context[TokenRole]](request)
          } yield response.map(_.data)

        /**
          * Creates (or replaces) the named role. Roles enforce specific behavior when creating tokens that allow
          * token functionality that is otherwise not available or would require sudo/root privileges to access.
          * Role parameters, when set, override any provided options to the create endpoints.
          * The role name is also included in the token path, allowing all tokens created against a role to be revoked
          * using the /sys/leases/revoke-prefix endpoint.
          * @param role the role to create/update.
          */
        def +=(role: TokenRole): F[Unit] = create(role)

        /**
          * Creates (or replaces) the named role. Roles enforce specific behavior when creating tokens that allow
          * token functionality that is otherwise not available or would require sudo/root privileges to access.
          * Role parameters, when set, override any provided options to the create endpoints.
          * The role name is also included in the token path, allowing all tokens created against a role to be revoked
          * using the /sys/leases/revoke-prefix endpoint.
          * @param role the role to create/update.
          */
        def create(role: TokenRole): F[Unit] = client.expectUnit(POST(role, rolesUri / role.name, tokenHeader))

        /**
          * Deletes the token role represented by `role`.
          * @param role the token role to delete.
          */
        def -=(role: TokenRole): F[Unit] = delete(role)

        /**
          * Deletes the token role represented by `role`.
          * @param role the token role to delete.
          */
        def delete(role: TokenRole): F[Unit] = delete(role.name)

        /**
          * Deletes the token role with `name`.
          * @param name the name of the token role to delete.
          */
        def -=(name: String): F[Unit] = delete(name)

        /**
          * Deletes the token role with `name`.
          * @param name the name of the token role to delete.
          */
        def delete(name: String): F[Unit] = client.expectUnit(DELETE(rolesUri / name, tokenHeader))
      }
    }
  }
}