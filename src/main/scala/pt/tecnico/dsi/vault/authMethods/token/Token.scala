package pt.tecnico.dsi.vault.authMethods.token

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.vault._
import pt.tecnico.dsi.vault.authMethods.token.models.{CreateOptions, Role, Token => MToken}

class Token[F[_]: Sync](uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** @return a list with all token accessor. This requires sudo capability, and access to it should be tightly
    *         controlled as the accessors can be used to revoke very large numbers of tokens and their associated
    *         leases at once. */
  def accessors(): F[List[String]] = executeWithContextKeys(LIST(uri / "accessors", token))

  /**
    * Creates a new token. Certain options are only available when called by a root token.
    * Orphan tokens require a root token to be created. However invoking `createOrphan` allows one to be created
    * without a root token.
    * If a role is used, the token will be created against the specified role name; this may override options set during this call.
    *
    * @param options the options to use while creating the token.
    * @return the created token and its properties.
    */
  def create(options: CreateOptions): F[Auth] = executeWithContextAuth(POST(options, uri / "create", token))

  /**
    * Creates a new token. Certain options are only available when called by a root token.
    * If a role is used, the token will be created against the specified role name; this may override options set during this call.
    *
    * @param options the options to use while creating the token.
    * @return the created token and its properties
    */
  def createOrphan(options: CreateOptions): F[Auth] = executeWithContextAuth(POST(options, uri / "create-orphan", token))

  /**
    * Creates a new token. Certain options are only available when called by a root token.
    * If a role is used, the token will be created against the specified role name; this may override options set during this call.
    *
    * @param options the options to use while creating the token.
    * @return the created token and its properties
    */
  def createRole(role: String, options: CreateOptions): F[Auth] = executeWithContextAuth(POST(options, uri / "create" / role, token))

  /** @param token the token for which to retrieve information.
    * @return returns information about the `token`.
    */
  def lookup(token: String): F[MToken] =
    executeWithContextData(POST(Map("token" -> token).asJson, uri / "lookup", this.token))

  /** @return returns information about the current client token.
    */
  def lookupSelf(): F[MToken] =
    executeWithContextData(POST(uri / "lookup-self", token))

  /** @param accessor the token accessor for which to retrieve the information.
    * @return returns information about the client token from the `accessor`.
    */
  def lookupAccessor(accessor: String): F[MToken] =
    executeWithContextData(POST(Map("accessor" -> accessor).asJson, uri / "lookup", token))

  /**
    * Renews a lease associated with a token. This is used to prevent the expiration of a token, and the automatic
    * revocation of it. Token renewal is possible only if there is a lease associated with it.
    *
    * @param token     the token to renew.
    * @param increment An optional requested lease increment can be provided. This increment may be ignored.
    * @return the renewed token information.
    */
  def renew(token: String, increment: Option[Int] = None): F[Auth] = {
    case class Renew(token: String, increment: Option[Int])
    executeWithContextAuth(POST(Renew(token, increment), uri / "renew", this.token))
  }

  /**
    * Renews a lease associated with the calling token. This is used to prevent the expiration of a token, and the
    * automatic revocation of it. Token renewal is possible only if there is a lease associated with it.
    *
    * @param increment An optional requested lease increment can be provided. This increment may be ignored.
    * @return the renewed token information.
    */
  def renewSelf(increment: Option[Int] = None): F[Auth] =
    executeWithContextAuth(POST(Map("increment" -> increment).asJson, uri / "renew-self", token))

  /**
    * Revokes a token and all child tokens. When the token is revoked, all dynamic secrets generated with it are also revoked.
    *
    * @param token the token to revoke.
    */
  def revoke(token: String): F[Unit] = execute(POST(Map("token" -> token).asJson, uri / "revoke", this.token))

  /** Revokes the token used to call it and all child tokens. When the token is revoked,
    * all dynamic secrets generated with it are also revoked. */
  def revokeSelf(): F[Unit] = execute(POST(uri / "revoke-self", token))

  /**
    * Revoke the token associated with the accessor and all the child tokens. This is meant for purposes where there
    * is no access to token ID but there is need to revoke a token and its children.
    *
    * @param accessor Accessor of the token.
    */
  def revokeAccessor(accessor: String): F[Unit] =
    execute(POST(Map("accessor" -> accessor).asJson, uri / "revoke-accessor", token))

  /**
    * Revokes a token but not its child tokens. When the token is revoked, all secrets generated with it are also revoked.
    * All child tokens are orphaned, but can be revoked sub-sequently using /auth/token/revoke/. This is a root-protected endpoint.
    *
    * @param token Token to revoke.
    */
  def revokeTokenAndOrphanChildren(token: String): F[Unit] =
    execute(POST(Map("token" -> token).asJson, uri / "revoke-orphan", this.token))

  object roles {
    private val rolesUri = uri / "roles"

    /**
      * @return a list with the names of the available token roles. To get the `TokenRoles` you can do:
      *         {{{
      *            import cats.implicits._
      *            val roles = client.authMethods.token.roles
      *            roles.list().flatMap(_.map(roles.apply).sequence)
      *         }}}
      */
    def list(): F[List[String]] = executeWithContextKeys(LIST(rolesUri, token))

    /**
      * Fetches the named role configuration.
      *
      * @param name the name of the role to fetch
      * @return if a role named `name` exists a `Some` will be returned with its configuration. `None` otherwise.
      */
    def get(name: String): F[Option[Role]] =
      for {
        request <- GET(rolesUri / name, token)
        response <- client.expectOption[Context[Role]](request)
      } yield response.map(_.data)
    /**
      * Fetches the named role configuration.
      *
      * @param name the name of the role to fetch
      * @return
      */
    def apply(name: String): F[Role] = get(name).map(_.get)

    /**
      * Creates (or replaces) the named role. Roles enforce specific behavior when creating tokens that allow
      * token functionality that is otherwise not available or would require sudo/root privileges to access.
      * Role parameters, when set, override any provided options to the create endpoints.
      * The role name is also included in the token path, allowing all tokens created against a role to be revoked
      * using the /sys/leases/revoke-prefix endpoint.
      *
      * @param role the role to create/update.
      */
    def create(role: Role): F[Unit] = execute(POST(role, rolesUri / role.name, token))
    /**
      * Alternative syntax to create a role:
      * * {{{ client.authMethods.token.roles += Role(...) }}}
      */
    def +=(role: Role): F[Unit] = create(role)
    /**
      * Allows creating multiple roles in one go:
      * {{{
      *   client.authMethods.token.roles ++= List(
      *     Role(...),
      *     Role(...),
      *   )
      * }}}
      */
    def ++=(roles: List[Role]): F[List[Unit]] = {
      import cats.instances.list._
      roles.map(create).sequence
    }

    /**
      * Deletes the token role with `name`.
      *
      * @param name the name of the token role to delete.
      */
    def delete(name: String): F[Unit] = execute(DELETE(rolesUri / name, token))
    /**
      * Alternative syntax to delete a role:
      * * {{{ client.authMethods.token.roles -= "my-role" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple roles in one go:
      * {{{
      *   client.authMethods.token.roles --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[List[Unit]] = {
      import cats.instances.list._
      names.map(delete).sequence
    }
  }

  /**
    * Iterates through all token accessors, looks up the accessor and only returns the ones containing the root policy.
    * This is a very heavy operation. It should normally be invoked with a root token. And it is very helpful to clean
    * dangling root tokens.
    */
  def listAccessorsWithRootPolicy: F[List[MToken]] =
    for {
      listAccessors <- accessors()
      listTokens <- listAccessors.traverse(lookupAccessor)
    } yield listTokens.collect { case t if t.policies.contains("root") => t }
}
