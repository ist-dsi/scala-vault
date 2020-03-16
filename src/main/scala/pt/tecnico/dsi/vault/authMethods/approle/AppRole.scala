package pt.tecnico.dsi.vault.authMethods.approle

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.functor._
import io.circe.syntax._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.vault.{Auth, DSL}
import pt.tecnico.dsi.vault.authMethods.approle.models._

class AppRole[F[_]: Sync](val path: String, val uri: Uri)(implicit client: Client[F], token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  object roles {
    private val rolesUri = uri / "role"

    /** List the existing roles. */
    def list(): F[List[String]] = executeWithContextKeys(LIST(rolesUri, token))

    /**
      * Reads the properties of the specified role.
      * @param name the name of the role to read.
      */
    def get(name: String): F[Option[Role]] = executeOptionWithContextData(GET(rolesUri / name, token))
    def apply(name: String): F[Role] = executeWithContextData(GET(rolesUri / name, token))

    /**
      * Creates a new AppRole or updates an existing AppRole.
      * This endpoint supports both `create` and `update` capabilities.
      * There can be one or more constraints enabled on the role.
      * It is required to have at least one of them enabled while creating or updating a role.
      * @param name the name of the new role.
      * @param role the role to create.
      */
    def create(name: String, role: Role): F[Unit] = execute(POST(role.asJson, rolesUri / name, token))
    /**
      * Alternative syntax to create a role:
      * * {{{ client.authMethods.approle("path").roles += "a" -> Role(...) }}}
      */
    def +=(tuple: (String, Role)): F[Unit] = create(tuple._1, tuple._2)
    /**
      * Allows creating multiple roles in one go:
      * {{{
      *   client.authMethods.approle("path").roles ++= List(
      *     "a" -> Role(...),
      *     "b" -> Role(...),
      *   )
      * }}}
      */
    def ++=(list: List[(String, Role)]): F[Unit] = list.map(+=).sequence_

    /**
      * Deletes the specified role.
      * @param name the name of the role to delete.
      */
    def delete(name: String): F[Unit] = execute(DELETE(rolesUri / name, token))
    /**
      * Alternative syntax to delete a role:
      * {{{ client.authMethods.approle("path").roles -= "role-name" }}}
      */
    def -=(name: String): F[Unit] = delete(name)
    /**
      * Allows deleting multiple roles in one go:
      * {{{
      *   client.authMethods.approle("path").roles --= List("a", "b")
      * }}}
      */
    def --=(names: List[String]): F[Unit] = names.map(delete).sequence_
  }

  // TODO: find a better name for this
  class AppRoleRole(uri: Uri) {
    /**
      * Performs some maintenance tasks to clean up invalid entries that may remain in the token store.
      * Generally, running this is not needed unless upgrade notes or support personnel suggest it.
      * This may perform a lot of I/O to the storage method so should be used sparingly.
      */
    val tidy: F[Unit] = execute(POST(uri / "tidy" / "secret-id", token))

    val roleId = new {
      private val roleUri = uri / "role-id"

      /** @return the RoleID of this AppRole role. */
      def apply(): F[String] = executeWithContextData[RoleId](GET(roleUri, token)).map(_.roleId)

      /**
        * Updates the RoleID of this AppRole role to a custom value.
        * @param newRoleId the new RoleID.
        */
      def update(newRoleId: String): F[Unit] = execute(POST(RoleId(newRoleId).asJson, roleUri, token))
    }
    val secretId = new {
      private val secretUri = uri / "secret-id"

      /**
        * Lists the accessors of all the SecretIDs issued against this AppRole role.
        * This includes the accessors for "custom" SecretIDs as well.
        */
      def listAccessors(): F[List[String]] = executeWithContextKeys(LIST(secretUri, token))

      /**
        * Generates and issues a new SecretID on an existing AppRole.
        *
        * Similar to tokens, the response will also contain a `secret_id_accessor` value which can be used to read the
        * properties of the SecretID without divulging the SecretID itself, and also to delete the SecretID from the AppRole.
        *
        * @param properties the secret id properties to use while generating the new secret id.
        */
      def generate(properties: SecretIdProperties): F[SecretIdResponse] = {
        executeWithContextData[SecretIdResponse](POST.apply(properties.asJson, secretUri, token))
      }

      /**
        * Assigns a "custom" SecretID against an existing AppRole. This is used in the "Push" model of operation.
        * @param secretId secretID to be attached to the Role.
        * @param properties the secret id properties to use while generating the new secret id.
        */
      def createCustom(secretId: String, properties: SecretIdProperties): F[SecretIdResponse] = {
        val body = SecretIdProperties.codec(properties).mapObject(_.add("secret_id", secretId.asJson))
        executeWithContextData[SecretIdResponse](POST(body, secretUri, token))
      }

      /**
        * Reads out the properties of a SecretID.
        *
        * @param secretId the secret id to read the properties from.
        */
      def get(secretId: String): F[Option[SecretIdProperties]] =
        executeOptionWithContextData(POST(Map("secret_id" -> secretId).asJson, secretUri / "lookup", token))
      def apply(secretId: String): F[SecretIdProperties] =
        executeWithContextData(POST(Map("secret_id" -> secretId).asJson, secretUri / "lookup", token))

      /**
        * Destroy an secret ID.
        * @param secretId the secret id to destroy.
        */
      def delete(secretId: String): F[Unit] = execute(POST(Map("secret_id" -> secretId).asJson, secretUri / "destroy", token))
      /**
        * Alternative syntax to delete a secret id:
        * {{{ client.auth.approle.role("my-role") -= "secret-id" }}}
        */
      def -=(secretId: String): F[Unit] = delete(secretId)

      /**
        * Reads out the properties of a SecretID from its accessor.
        *
        * @param accessor the secret id accessor to read the properties from.
        */
      def getUsingAccessor(accessor: String): F[Option[SecretIdProperties]] =
        executeOptionWithContextData(POST(Map("secret_id_accessor" -> accessor).asJson, uri / "secret-id-accessor" / "lookup", token))

      /**
        * Destroy an secret ID using its accessor.
        * @param accessor the secret id accessor to use to destroy the corresponding secret id.
        */
      def deleteUsingAccesor(accessor: String): F[Unit] =
        execute(POST(Map("secret_id_accessor" -> accessor).asJson, uri / "secret-id-accessor" / "destroy", token))
    }
  }
  //TODO: maybe this should return an Option
  def role(id: String): AppRoleRole = new AppRoleRole(uri / "role" / id)

  /**
    * Issues a Vault token based on the presented credentials.
    * `roleId` is always required; if `bind_secret_id` is enabled (the default) on the AppRole, `secret_id` is required too.
    * Any other bound authentication values on the AppRole (such as client IP CIDR) are also evaluated.
    * @param roleId the role id to use.
    * @param secretId the secret id to use.
    */
  def login(roleId: String, secretId: String): F[Auth] =
    executeWithContextAuth(POST(Map("role_id" -> roleId, "secret_id" -> secretId).asJson, uri / "login"))
}
