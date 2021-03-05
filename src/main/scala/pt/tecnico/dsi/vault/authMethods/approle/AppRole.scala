package pt.tecnico.dsi.vault.authMethods.approle

import cats.effect.Concurrent
import cats.syntax.functor._
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{GET, POST}
import pt.tecnico.dsi.vault.{Auth, DSL, RolesCRUD}
import pt.tecnico.dsi.vault.authMethods.approle.models._

final class AppRole[F[_]: Concurrent: Client](val path: String, val uri: Uri)(implicit token: Header.Raw) { self =>
  private val dsl = new DSL[F] {}
  import dsl._

  object roles extends RolesCRUD[F, Role](path, uri) {
    override val path: String = s"${self.path}/role"
    override val uri: Uri = self.uri / "role"
  }

  def role(id: String): AppRoleRole = new AppRoleRole(id)
  final class AppRoleRole(val id: String) { innerSelf =>
    val path: String = s"${self.path}/role/$id"
    val uri: Uri = self.uri / "role" / id
    /**
      * Performs some maintenance tasks to clean up invalid entries that may remain in the token store.
      * Generally, running this is not needed unless upgrade notes or support personnel suggest it.
      * This may perform a lot of I/O to the storage method so should be used sparingly.
      */
    val tidy: F[Unit] = execute(POST(uri / "tidy" / "secret-id", token))

    object roleId {
      val path: String = s"${innerSelf.path}/role-id"
      val uri: Uri = innerSelf.uri / "role-id"

      /** @return the RoleID of this AppRole role. */
      def apply(): F[String] = executeWithContextData[RoleId](GET(uri, token)).map(_.roleId)

      /**
        * Updates the RoleID of this AppRole role to a custom value.
        * @param newRoleId the new RoleID.
        */
      def update(newRoleId: String): F[Unit] = execute(POST(RoleId(newRoleId), uri, token))
    }
    object secretId {
      val path: String = s"${innerSelf.path}/secret-id"
      val uri: Uri = innerSelf.uri / "secret-id"

      /**
        * Lists the accessors of all the SecretIDs issued against this AppRole role.
        * This includes the accessors for "custom" SecretIDs as well.
        */
      val listAccessors: F[List[String]] = executeWithContextKeys(LIST(uri, token))

      /**
        * Generates and issues a new SecretID on an existing AppRole.
        *
        * Similar to tokens, the response will also contain a `secret_id_accessor` value which can be used to read the
        * properties of the SecretID without divulging the SecretID itself, and also to delete the SecretID from the AppRole.
        *
        * @param properties the secret id properties to use while generating the new secret id.
        */
      def generate(properties: SecretIdProperties): F[SecretIdResponse] = {
        executeWithContextData[SecretIdResponse](POST(properties, uri, token))
      }

      /**
        * Assigns a "custom" SecretID against an existing AppRole. This is used in the "Push" model of operation.
        * @param secretId secretID to be attached to the Role.
        * @param properties the secret id properties to use while generating the new secret id.
        */
      def createCustom(secretId: String, properties: SecretIdProperties): F[SecretIdResponse] = {
        import io.circe.syntax._
        val body = SecretIdProperties.codec(properties).mapObject(_.add("secret_id", secretId.asJson))
        executeWithContextData[SecretIdResponse](POST(body, uri, token))
      }

      /**
        * Reads out the properties of a SecretID.
        *
        * @param secretId the secret id to read the properties from.
        */
      def get(secretId: String): F[Option[SecretIdProperties]] =
        executeOptionWithContextData(POST(Map("secret_id" -> secretId), uri / "lookup", token))
      def apply(secretId: String): F[SecretIdProperties] =
        executeWithContextData(POST(Map("secret_id" -> secretId), uri / "lookup", token))

      /**
        * Destroy an secret ID.
        * @param secretId the secret id to destroy.
        */
      def delete(secretId: String): F[Unit] = execute(POST(Map("secret_id" -> secretId), uri / "destroy", token))

      /**
        * Reads out the properties of a SecretID from its accessor.
        *
        * @param accessor the secret id accessor to read the properties from.
        */
      def getUsingAccessor(accessor: String): F[Option[SecretIdProperties]] =
        executeOptionWithContextData(POST(Map("secret_id_accessor" -> accessor), uri / "secret-id-accessor" / "lookup", token))

      /**
        * Destroy an secret ID using its accessor.
        * @param accessor the secret id accessor to use to destroy the corresponding secret id.
        */
      def deleteUsingAccesor(accessor: String): F[Unit] =
        execute(POST(Map("secret_id_accessor" -> accessor), uri / "secret-id-accessor" / "destroy", token))
    }
  }

  /**
    * Issues a Vault token based on the presented credentials.
    * `roleId` is always required; if `bind_secret_id` is enabled (the default) on the AppRole, `secret_id` is required too.
    * Any other bound authentication values on the AppRole (such as client IP CIDR) are also evaluated.
    * @param roleId the role id to use.
    * @param secretId the secret id to use.
    */
  def login(roleId: String, secretId: String): F[Auth] =
    executeWithContextAuth(POST(Map("role_id" -> roleId, "secret_id" -> secretId), uri / "login"))
}
