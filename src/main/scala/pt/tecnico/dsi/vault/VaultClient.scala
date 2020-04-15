package pt.tecnico.dsi.vault

import cats.effect.Sync
import io.circe.Decoder
import org.http4s._
import org.http4s.client.Client

//TODO: Some operations don't require a token. So the fact that we are requiring one might be misleading
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

class VaultClient[F[_]: Sync](val baseUri: Uri, val token: String)(implicit client: Client[F]) { self =>
  implicit val tokenHeader: Header = Header("X-Vault-Token", token)

  val uri = baseUri / "v1"

  object sys {
    val uri: Uri = self.uri / "sys"

    import pt.tecnico.dsi.vault.sys._

    val init = new Init[F](uri / "init") // Does not require a token
    val health = new Health[F](uri / "health") // Does not require a token
    val leader = new Leader[F](uri) // One endpoint requires a token, the other does not
    val seal = new Seal[F](uri) // Does not require a token
    val generateRoot = new GenerateRoot[F](uri / "generate-root") // Does not require a token
    val leases = new Leases[F]("sys/leases", uri / "leases")
    val policy = new Policy[F]("sys/policy", uri / "policy")
    val auth = new Auth[F]("sys/auth", uri / "auth", self)
    val mounts = new Mounts[F]("sys/mounts", uri / "mounts", self)
    val keys = new Keys[F](uri) // One endpoint requires a token, the other does not
    val rekey = new Rekey[F](uri / "rekey") // Does not require a token
    val pluginCatalog = new PluginCatalog[F]("sys/plugins/catalog", uri / "plugins" / "catalog")
  }

  object authMethods {
    val uri: Uri = self.uri / "auth"

    import pt.tecnico.dsi.vault.authMethods.approle.AppRole
    import pt.tecnico.dsi.vault.authMethods.token.Token

    // The Token auth method is always mounted at this location. And cannot be changed.
    val token: Token[F] = new Token[F]("auth/token", uri / "token")
    def appRole(at: String = "approle"): AppRole[F] = new AppRole[F](s"auth/$at", uri append at)
  }

  object secretEngines {
    import pt.tecnico.dsi.vault.secretEngines.consul.Consul
    import pt.tecnico.dsi.vault.secretEngines.kv.KeyValueV1
    import pt.tecnico.dsi.vault.secretEngines.pki.PKI
    import pt.tecnico.dsi.vault.secretEngines.databases._

    def kv(at: String = "kv"): KeyValueV1[F] = new KeyValueV1[F](at, uri append at)
    def consul(at: String = "consul"): Consul[F] = new Consul[F](at, uri append at)
    def pki(at: String = "pki"): PKI[F] = new PKI[F](at, uri append at)
    def mysql(at: String = "database"): MySql[F] = new MySql[F](at, uri append at)
    def mongodb(at: String = "database"): MongoDB[F] = new MongoDB[F](at, uri append at)
    def elasticsearch(at: String = "database"): Elasticsearch[F] = new Elasticsearch[F](at, uri append at)
  }

  private val dsl = new DSL[F] {}
  import dsl._

  def write[A: Decoder](path: String, body: A)(implicit aEncoder: EntityEncoder[F, A]): F[Context[A]] =
    client.expect(PUT(body, uri append path, tokenHeader))

  def read[A: Decoder](path: String)(implicit contextDecoder: EntityDecoder[F, Context[A]]): F[Context[A]] =
    client.expect(GET(uri append path, tokenHeader))

  def list(path: String): F[List[String]] = executeWithContextKeys(LIST(uri append path))

  def delete(path: String): F[Unit] = execute(DELETE(uri append path))
}
