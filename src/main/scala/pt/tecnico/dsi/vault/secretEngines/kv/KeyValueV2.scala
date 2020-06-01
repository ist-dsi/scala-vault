package pt.tecnico.dsi.vault.secretEngines.kv

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import org.http4s.Method.{DELETE, GET, POST, PUT}
import pt.tecnico.dsi.vault.DSL
import pt.tecnico.dsi.vault.secretEngines.kv.models.{Configuration, Metadata, Secret, VersionMetadata}

final class KeyValueV2[F[_]: Sync: Client](val path: String, val uri: Uri)(implicit token: Header) {
  private val dsl = new DSL[F] {}
  import dsl._

  /** Sets backend level configurations that are applied to every key in the key-value store. */
  def configure(configuration: Configuration): F[Unit] = execute(POST(configuration, uri / "config", token))
  /** Retrieves the current backend level configuration. */
  val configuration: F[Configuration] = executeWithContextData(GET(uri / "config", token))

  /**
    * Configures the metadata for the secret at the specified path.
    * If no secret exists at `path` a new one will be created.
    * If the value does not yet exist, the calling token must have an ACL policy granting the  create capability.
    * If the value already exists, the calling token must have an ACL policy granting the update capability.
    * @param path the path to update the metadata.
    * @param configuration the metadata to update
    */
  def configureAt(path: String, configuration: Configuration): F[Unit] = execute(POST(configuration, uri / "metadata" / path, token))
  /** Alias for `configureAt`.
    * This method exists to make this API similar to the Vault API documentation. However the name is misleading. */
  def updateMetadata(path: String, configuration: Configuration): F[Unit] = configureAt(path, configuration)
  /**
    * Retrieves the metadata and versions for the secret at the specified path.
    * @param path the path of the secret to read.
    */
  def readMetadata(path: String): F[Metadata] = execute(GET(uri / "metadata" / path, token))

  /**
    * Returns a list of key names at the specified location. Folders are suffixed with /.
    * The input must be a folder; list on a file will not return a value.
    * Note that no policy-based filtering is performed on keys; do not encode sensitive information in key names.
    * The values themselves are not accessible via this command.
    * @param path the path to list the secrets keys.
    */
  def list(path: String): F[List[String]] = executeWithContextKeys(LIST(uri / "metadata" / path, token))

  /**
    * Reads the secret data and metadata at `path`.
    * @param path the path from which to retrieve the secret.
    * @param version the version to return. If not set the latest version is returned.
    * @tparam A the type of the secret data.
    */
  def readWithVersion[A: Decoder](path: String, version: Option[Int] = None): F[Option[Secret[A]]] =
    executeOptionWithContextData(GET(uri / "data" / path +??("version", version), token))
  /**
    * Retrieves the secret data at `path`.
    * @param path the path from which to retrieve the secret data.
    * @param version the version to return. If not set the latest version is returned.
    * @tparam A the type of the secret data.
    */
  def read[A: Decoder](path: String, version: Option[Int] = None): F[Option[A]] = readWithVersion(path, version).map(_.map(_.data))
  /**
    * Retrieves the secret data at `path` assuming it exists.
    * @param path the path from which to retrieve the secret data.
    * @param version Specifies the version to return. If not set the latest version is returned.
    * @tparam A the type of the secret data.
    */
  def apply[A: Decoder](path: String, version: Option[Int] = None): F[A] =
    executeWithContextData[Secret[A]](GET(uri / "data" / path +??("version", version), token)).map(_.data)

  /**
    * Stores a secret of type `A` at `path`. Given that `A` can be encoded as a Json Object.
    * If the value does not yet exist, the calling token must have an ACL policy granting the `create` capability.
    * If the value already exists, the calling token must have an ACL policy granting the `update` capability.
    *
    * @param path the path at which to create the secret.
    * @param secret the secret.
    * @param cas Set the "cas" value to use a Check-And-Set operation. If not set the write will be allowed.
    *            If set to 0 a write will only be allowed if the key doesn’t exist. If the index is non-zero the write will only be allowed
    *            if the key’s current version matches the version specified in the cas parameter.
    * @tparam A the type of the secret to be created.
    */
  def write[A: Encoder.AsObject](path: String, secret: A, cas: Option[Int] = None): F[VersionMetadata] = {
    import io.circe.syntax._
    val body = Map(
      "options" -> cas.map(value => Map("cas" -> value)).getOrElse(Map.empty).asJson,
      "data" -> secret.asJson,
    )
    executeWithContextData(PUT(body, uri / "data" / path, token))
  }

  /**
    * Issues a soft delete of the specified versions of the secret.
    * This marks the versions as deleted and will stop them from being returned from reads, but the underlying data will not be removed.
    *
    * A delete can be undone using the `undelete`.
    * @param path the path of the secret to delete
    * @param versions The versions to be deleted. The versioned data will not be deleted, but it will no longer be returned in normal get requests.
    */
  def deleteVersions(path: String, versions: List[Int]): F[Unit] = execute(POST(versions, uri / "delete" / path, token))
  /**
    * Undeletes the data for the provided version and path in the key-value store. This restores the data, allowing it to be returned on get requests.
    * @param path the path of the secret to undelete
    * @param versions The versions to undelete. The versions will be restored and their data will be returned on normal get requests.
    */
  def undeleteVersions(path: String, versions: List[Int]): F[Unit] = execute(POST(versions, uri / "undelete" / path, token))
  /**
    * Permanently removes the specified version data for the provided key and version numbers from the key-value store.
    * @param path the path of the secret to destroy.
    * @param versions the versions to destroy. Their data will be permanently deleted.
    */
  def destroyVersions(path: String, versions: List[Int]): F[Unit] = execute(POST(versions, uri / "destroy" / path, token))

  /**
    * Issues a soft delete of the secret's latest version at the specified `path`.
    * This marks the version as deleted and will stop it from being returned from reads, but the underlying data will not be removed.
    * A delete can be undone using the undelete path.
    * @param path the path of the secret to delete
    */
  def delete(path: String): F[Unit] = execute(DELETE(uri / "data" / path, token))
  /**
    * Permanently deletes the key metadata and all version data for the specified key. All version history will be removed.
    * @param path the path of the secret to delete
    */
  def destroy(path: String): F[Unit] = execute(DELETE(uri / "metadata" / path, token))
}