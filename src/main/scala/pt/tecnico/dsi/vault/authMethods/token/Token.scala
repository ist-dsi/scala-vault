package pt.tecnico.dsi.vault.authMethods.token

import scala.concurrent.duration.FiniteDuration
import cats.effect.Concurrent
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.syntax._
import io.circe.Json
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.POST
import org.http4s.Status.Successful
import pt.tecnico.dsi.vault.{Auth, Context, DSL, RolesCRUD}
import pt.tecnico.dsi.vault.authMethods.token.models.{CreateOptions, Role, Token => MToken}

final class Token[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header) {
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

  // Usually when something does not exist a REST API returns a 404, but not this one.
  private def lookup(thingName: String, thing: String, uri: Uri): F[Option[MToken]] =
    genericExecute(POST(Map(thingName -> thing), uri, token))({
      case Successful(response) => response.as[Context[MToken]].map(context => Option(context.data))
    }, {
      case errors if errors.exists(_.contains(s"invalid $thingName")) => Option.empty[MToken].pure[F]
    })

  /** @param token the token for which to retrieve information.
    * @return returns information about the `token`.
    */
  def lookup(token: String): F[Option[MToken]] = lookup("token", token, uri / "lookup")

  /** @return returns information about the current client token.
    */
  def lookupSelf(): F[MToken] = executeWithContextData(POST(uri / "lookup-self", token))

  /** @param accessor the token accessor for which to retrieve the information.
    * @return returns information about the client token from the `accessor`.
    */
  def lookupAccessor(accessor: String): F[Option[MToken]] = lookup("accessor", accessor, uri / "lookup-accessor")


  //Unfortunately the endpoints using this are expecting an Int, so we cannot use increment.asJson
  private def incrementToJson(increment: Option[FiniteDuration]): Json = increment.map(_.toSeconds.toInt).asJson

  /**
    * Renews a lease associated with a token. This is used to prevent the expiration of a token, and the automatic
    * revocation of it. Token renewal is possible only if there is a lease associated with it.
    *
    * @param token     the token to renew.
    * @param increment An optional requested lease increment can be provided. This increment may be ignored.
    * @return the renewed token information.
    */
  def renew(token: String, increment: Option[FiniteDuration] = None): F[Auth] = {
    val body = Map("token" -> token.asJson, "increment" -> incrementToJson(increment))
    executeWithContextAuth(POST(body, uri / "renew", this.token))
  }

  /**
    * Renews a lease associated with the calling token. This is used to prevent the expiration of a token, and the
    * automatic revocation of it. Token renewal is possible only if there is a lease associated with it.
    *
    * @param increment An optional requested lease increment can be provided. This increment may be ignored.
    * @return the renewed token information.
    */
  def renewSelf(increment: Option[FiniteDuration] = None): F[Auth] = executeWithContextAuth(POST(Map("increment" -> incrementToJson(increment)), uri / "renew-self", token))

  /**
    * Renews a lease associated with a token using its accessor. This is used to prevent the expiration of a token, and the automatic revocation of it.
    * Token renewal is possible only if there is a lease associated with it.
    * @param accessor Accessor associated with the token to renew.
    * @param increment An optional requested lease increment can be provided.
    * @return returns information about the client token from the `accessor`.
    */
  def renewAccessor(accessor: String, increment: Option[FiniteDuration]): F[Auth] = {
    val body = Map("accessor" -> accessor.asJson, "increment" -> incrementToJson(increment))
    executeWithContextData(POST(body, uri / "renew-accessor", token))
  }


  /**
    * Revokes a token and all child tokens. When the token is revoked, all dynamic secrets generated with it are also revoked.
    *
    * @param token the token to revoke.
    */
  def revoke(token: String): F[Unit] = execute(POST(Map("token" -> token), uri / "revoke", this.token))

  /** Revokes the token used to call it and all child tokens. When the token is revoked,
    * all dynamic secrets generated with it are also revoked. */
  def revokeSelf(): F[Unit] = execute(POST(uri / "revoke-self", token))

  /**
    * Revoke the token associated with the accessor and all the child tokens. This is meant for purposes where there
    * is no access to token ID but there is need to revoke a token and its children.
    *
    * @param accessor Accessor of the token.
    */
  def revokeAccessor(accessor: String): F[Unit] = execute(POST(Map("accessor" -> accessor), uri / "revoke-accessor", token))

  /**
    * Revokes a token but not its child tokens. When the token is revoked, all secrets generated with it are also revoked.
    * All child tokens are orphaned, but can be revoked sub-sequently using /auth/token/revoke/. This is a root-protected endpoint.
    *
    * @param token Token to revoke.
    */
  def revokeTokenAndOrphanChildren(token: String): F[Unit] = execute(POST(Map("token" -> token), uri / "revoke-orphan", this.token))

  object roles extends RolesCRUD[F, Role](path, uri) {
    /**
      * Creates (or replaces) the named role. Roles enforce specific behavior when creating tokens that allow
      * token functionality that is otherwise not available or would require sudo/root privileges to access.
      * Role parameters, when set, override any provided options to the create endpoints.
      * The role name is also included in the token path, allowing all tokens created against a role to be revoked
      * using the /sys/leases/revoke-prefix endpoint.
      *
      * @param role the role to create/update.
      */
    override def create(name: String, role: Role): F[Unit] = super.create(name, role)
  }

  /**
    * Iterates through all token accessors, looks up the accessor and only returns the ones containing the root policy.
    * This is a very heavy operation. It should normally be invoked with a root token. And it is very helpful to clean
    * dangling root tokens.
    */
  def listAccessorsWithRootPolicy: F[List[MToken]] =
    for {
      listAccessors <- accessors()
      listTokens <- listAccessors.flatTraverse(accessor => lookupAccessor(accessor).map(_.toList))
    } yield listTokens.collect { case t if t.policies.contains("root") => t }
}
