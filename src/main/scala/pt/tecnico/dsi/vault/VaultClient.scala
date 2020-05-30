package pt.tecnico.dsi.vault

import cats.effect.Sync
import io.circe.Decoder
import org.http4s._
import org.http4s.client.Client

// Some operations don't require a token. So the fact that we are requiring one might be misleading.
// We could implement something like this:
/*object VaultClient {
  class VaultClientPublicEndpoints[F[_]: Sync](val baseUri: Uri)(implicit client: Client[F]) { self =>
    val uri = baseUri / "v1"

    object sys {
      val uri: Uri = self.uri / "sys"

      import pt.tecnico.dsi.vault.sys._

      val init = new Init[F](uri / "init")
      val health = new Health[F](uri / "health")
      //val leader = new Leader[F](uri) // One endpoint requires a token, the other does not
      val seal = new Seal[F](uri)
      val generateRoot = new GenerateRoot[F](uri / "generate-root")
      //val keys = new Keys[F](uri) // One endpoint requires a token, the other does not
      val rekey = new Rekey[F](uri / "rekey")
    }
  }

  def apply[F[_]: Sync](baseUri: Uri, token: String)(implicit client: Client[F]): VaultClient[F] =
    new VaultClient(baseUri, token)
  def apply[F[_]: Sync](baseUri: Uri)(implicit client: Client[F]): VaultClientPublicEndpoints[F] =
    new VaultClientPublicEndpoints(baseUri)
}*/
// But is it worth the extra complexity?

final class VaultClient[F[_]: Sync](val baseUri: Uri, val token: String)(implicit client: Client[F]) { self =>
  implicit val tokenHeader: Header = Header("X-Vault-Token", token)

  val uri: Uri = baseUri / "v1"

  object authMethods {
    val path: String = "auth"
    val uri: Uri = self.uri / path

    import pt.tecnico.dsi.vault.authMethods.approle.AppRole
    import pt.tecnico.dsi.vault.authMethods.token.Token

    // The Token auth method is always mounted at this location, and cannot be changed.
    val token: Token[F] = new Token[F](s"$path/token", uri / "token")
    def appRole(at: String = "approle"): AppRole[F] = new AppRole[F](s"$path/$at", uri / at)
  }

  object secretEngines {
    import pt.tecnico.dsi.vault.secretEngines.consul.Consul
    import pt.tecnico.dsi.vault.secretEngines.kv._
    import pt.tecnico.dsi.vault.secretEngines.pki.PKI
    import pt.tecnico.dsi.vault.secretEngines.databases._

    def keyValueV1(at: String = "kv"): KeyValueV1[F] = new KeyValueV1[F](at, uri / at)
    def keyValueV2(at: String = "kv"): KeyValueV2[F] = new KeyValueV2[F](at, uri / at)
    def consul(at: String = "consul"): Consul[F] = new Consul[F](at, uri / at)
    def pki(at: String = "pki"): PKI[F] = new PKI[F](at, uri / at)
    def mysql(at: String = "database"): MySql[F] = new MySql[F](at, uri / at)
    def mongodb(at: String = "database"): MongoDB[F] = new MongoDB[F](at, uri / at)
    def elasticsearch(at: String = "database"): Elasticsearch[F] = new Elasticsearch[F](at, uri / at)
  }

  object sys {
    val path: String = "sys"
    val uri: Uri = self.uri / path

    import pt.tecnico.dsi.vault.sys._

    val init = new Init[F](s"$path/init", uri / "init") // Does not require a token
    val health = new Health[F](s"$path/health", uri / "health") // Does not require a token
    val leader = new Leader[F](uri) // One endpoint requires a token, the other does not
    val seal = new Seal[F](uri) // Does not require a token
    val generateRoot = new GenerateRoot[F](s"$path/generate-root", uri / "generate-root") // Does not require a token
    val leases = new Leases[F](s"$path/leases", uri / "leases")
    val policy = new Policy[F](s"$path/policy", uri / "policy")
    val auth = new Auth[F](s"$path/auth", uri / "auth", self)
    val mounts = new Mounts[F](s"$path/mounts", uri / "mounts", self)
    val keys = new Keys[F](uri) // One endpoint requires a token, the other does not
    val rekey = new Rekey[F](uri / "rekey") // Does not require a token
    val pluginCatalog = new PluginCatalog[F](s"$path/plugins/catalog", uri / "plugins" / "catalog")
  }

  private val dsl = new DSL[F] {}
  import dsl._

  def write[A: Decoder](path: String, body: A)(implicit aEncoder: EntityEncoder[F, A]): F[Context[A]] =
    client.expect(PUT(body, uri / path, tokenHeader))

  def read[A: Decoder](path: String)(implicit contextDecoder: EntityDecoder[F, Context[A]]): F[Context[A]] =
    client.expect(GET(uri / path, tokenHeader))

  def list(path: String): F[List[String]] = executeWithContextKeys(LIST(uri / path))

  def delete(path: String): F[Unit] = execute(DELETE(uri / path))
}
